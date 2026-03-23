package com.shakti.hisaab;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shakti.hisaab.adapters.MilkCalendarAdapter;
import com.shakti.hisaab.adapters.MilkEntryListAdapter;
import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.dao.MilkEntryDao;
import com.shakti.hisaab.database.entities.MilkEntry;
import com.shakti.hisaab.model.CalendarDay;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MilkCalendarActivity extends AppCompatActivity implements MilkCalendarAdapter.OnDayClickListener {
    private static final String PREFS_NAME = "hisaab_settings";
    private static final String PREF_RATE = "milk_rate";
    private static final String PREF_DEFAULT_QTY = "default_quantity";

    private YearMonth currentMonth = YearMonth.now();
    private MilkEntryDao milkEntryDao;
    private ExecutorService dbExecutor;

    private RecyclerView recyclerView;
    private TextView tvMonth;
    private TextView tvSummaryTitle;
    private TextView tvDaysTaken;
    private TextView tvTotalLiters;
    private TextView tvTotalAmount;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnBack;
    private ImageButton btnSettings;
    private Button btnAddToday;
    private Button btnViewEntries;
    private Button btnClearAll;

    private MilkCalendarAdapter adapter;
    private List<MilkEntry> monthEntries = new ArrayList<>();

    private double milkRate = 50.0;
    private double defaultQuantity = 0.5;

    private final DateTimeFormatter monthFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_milk_calendar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        milkEntryDao = AppDatabase.getInstance(this).milkEntryDao();
        dbExecutor = Executors.newSingleThreadExecutor();

        initViews();
        loadSettings();
        setupListeners();
        setupRecycler();
        loadMonthData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.calendarRecycler);
        tvMonth = findViewById(R.id.tvMonth);
        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvDaysTaken = findViewById(R.id.tvDaysTaken);
        tvTotalLiters = findViewById(R.id.tvTotalLiters);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);
        btnAddToday = findViewById(R.id.btnAddToday);
        btnViewEntries = findViewById(R.id.btnViewEntries);
        btnClearAll = findViewById(R.id.btnClearAll);
    }

    private void setupRecycler() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 7);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MilkCalendarAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnPrev.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            loadMonthData();
        });

        btnNext.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            loadMonthData();
        });

        btnBack.setOnClickListener(v -> finish());

        btnSettings.setOnClickListener(v -> openSettingsDialog());

        btnAddToday.setOnClickListener(v -> {
            LocalDate today = LocalDate.now();
            currentMonth = YearMonth.from(today);
            loadMonthData();
            openEntryDialog(today);
        });

        btnViewEntries.setOnClickListener(v -> openEntriesDialog());

        btnClearAll.setOnClickListener(v -> confirmClearAll());
    }

    private void loadMonthData() {
        String monthPrefix = currentMonth.toString();
        dbExecutor.execute(() -> {
            List<MilkEntry> entries = milkEntryDao.getEntriesForMonth(monthPrefix);
            Map<String, MilkEntry> entryMap = new HashMap<>();
            for (MilkEntry entry : entries) {
                entryMap.put(entry.date, entry);
            }

            List<CalendarDay> days = buildDays(entryMap);
            Summary summary = calculateSummary(entries);

            runOnUiThread(() -> {
                monthEntries = entries;
                tvMonth.setText(currentMonth.format(monthFormatter));
                tvSummaryTitle.setText(currentMonth.format(monthFormatter) + " Summary");
                tvDaysTaken.setText(String.valueOf(summary.daysTaken));
                tvTotalLiters.setText(summary.totalLiters);
                tvTotalAmount.setText(summary.totalAmount);
                adapter = new MilkCalendarAdapter(days, this);
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private List<CalendarDay> buildDays(Map<String, MilkEntry> entryMap) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        int leadingEmpty = firstDayOfWeek % 7;

        for (int i = 0; i < leadingEmpty; i++) {
            days.add(new CalendarDay(null, 0, false, CalendarDay.State.EMPTY, null));
        }

        LocalDate today = LocalDate.now();
        int length = currentMonth.lengthOfMonth();
        for (int day = 1; day <= length; day++) {
            LocalDate date = currentMonth.atDay(day);
            String key = date.toString();
            MilkEntry entry = entryMap.get(key);
            boolean isToday = date.equals(today);

            CalendarDay.State state;
            Double quantity = null;

            if (date.isAfter(today)) {
                state = CalendarDay.State.FUTURE;
            } else if (entry == null) {
                state = CalendarDay.State.NO_ENTRY;
            } else if (entry.taken) {
                state = entry.paid ? CalendarDay.State.PAID : CalendarDay.State.UNPAID;
                quantity = entry.quantity;
            } else {
                state = CalendarDay.State.NOT_TAKEN;
            }

            days.add(new CalendarDay(key, day, isToday, state, quantity));
        }

        return days;
    }

    private Summary calculateSummary(List<MilkEntry> entries) {
        int daysTaken = 0;
        double totalLiters = 0;
        double totalAmount = 0;

        for (MilkEntry entry : entries) {
            if (entry.taken) {
                daysTaken++;
                totalLiters += entry.quantity;
                totalAmount += entry.totalCost;
            }
        }

        return new Summary(daysTaken, totalLiters, totalAmount);
    }

    @Override
    public void onDayClick(CalendarDay day) {
        if (day.dateKey == null) {
            return;
        }
        openEntryDialog(LocalDate.parse(day.dateKey));
    }

    private void openEntryDialog(LocalDate date) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_milk_entry, null);
        TextView tvDialogDate = view.findViewById(R.id.tvDialogDate);
        RadioGroup rgTaken = view.findViewById(R.id.rgTaken);
        RadioButton rbTakenYes = view.findViewById(R.id.rbTakenYes);
        RadioButton rbTakenNo = view.findViewById(R.id.rbTakenNo);
        View layoutTakenFields = view.findViewById(R.id.layoutTakenFields);
        EditText etQuantity = view.findViewById(R.id.etQuantity);
        EditText etAmount = view.findViewById(R.id.etAmount);
        RadioGroup rgPayment = view.findViewById(R.id.rgPayment);
        RadioButton rbPaid = view.findViewById(R.id.rbPaid);
        RadioButton rbUnpaid = view.findViewById(R.id.rbUnpaid);
        Button btnDelete = view.findViewById(R.id.btnDeleteEntry);

        tvDialogDate.setText(date.toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                boolean taken = rbTakenYes.isChecked();
                double quantity = parseDouble(etQuantity.getText().toString(), defaultQuantity);
                double amount = parseDouble(etAmount.getText().toString(), quantity * milkRate);
                boolean paid = rbPaid.isChecked();

                MilkEntry entry;
                if (taken) {
                    entry = new MilkEntry(date.toString(), true, paid, quantity, milkRate, amount);
                } else {
                    entry = new MilkEntry(date.toString(), false, false, 0, milkRate, 0);
                }

                dbExecutor.execute(() -> {
                    milkEntryDao.insert(entry);
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        loadMonthData();
                        Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        });

        rgTaken.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTakenYes) {
                layoutTakenFields.setVisibility(View.VISIBLE);
            } else {
                layoutTakenFields.setVisibility(View.GONE);
            }
        });

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                double quantity = parseDouble(s.toString(), defaultQuantity);
                double amount = quantity * milkRate;
                etAmount.setText(String.format(Locale.getDefault(), "%.0f", amount));
            }
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete this entry?")
                    .setPositiveButton("Delete", (d, which) -> {
                        dbExecutor.execute(() -> {
                            milkEntryDao.deleteByDate(date.toString());
                            runOnUiThread(() -> {
                                dialog.dismiss();
                                loadMonthData();
                                Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dbExecutor.execute(() -> {
            MilkEntry entry = milkEntryDao.getEntryByDate(date.toString());
            runOnUiThread(() -> {
                if (entry != null) {
                    btnDelete.setVisibility(View.VISIBLE);
                    if (entry.taken) {
                        rbTakenYes.setChecked(true);
                        layoutTakenFields.setVisibility(View.VISIBLE);
                        etQuantity.setText(String.valueOf(entry.quantity));
                        etAmount.setText(String.valueOf(entry.totalCost));
                        if (entry.paid) {
                            rbPaid.setChecked(true);
                        } else {
                            rbUnpaid.setChecked(true);
                        }
                    } else {
                        rbTakenNo.setChecked(true);
                        layoutTakenFields.setVisibility(View.GONE);
                    }
                } else {
                    rbTakenYes.setChecked(true);
                    layoutTakenFields.setVisibility(View.VISIBLE);
                    etQuantity.setText(String.valueOf(defaultQuantity));
                    etAmount.setText(String.format(Locale.getDefault(), "%.0f", defaultQuantity * milkRate));
                    rbPaid.setChecked(true);
                }
            });
        });

        dialog.show();
    }

    private void openEntriesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_entries_list, null);
        TextView tvTitle = view.findViewById(R.id.tvEntriesTitle);
        TextView tvNoEntries = view.findViewById(R.id.tvNoEntries);
        RecyclerView rvEntries = view.findViewById(R.id.rvEntries);

        tvTitle.setText(currentMonth.format(monthFormatter) + " Entries");
        rvEntries.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        if (monthEntries.isEmpty()) {
            tvNoEntries.setVisibility(View.VISIBLE);
        } else {
            tvNoEntries.setVisibility(View.GONE);
        }

        AlertDialog entriesDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Close", (d, which) -> d.dismiss())
                .create();

        MilkEntryListAdapter listAdapter = new MilkEntryListAdapter(monthEntries, entry -> {
            entriesDialog.dismiss();
            openEntryDialog(LocalDate.parse(entry.date));
        });
        rvEntries.setAdapter(listAdapter);

        entriesDialog.show();
    }

    private void openSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        EditText etMilkRate = view.findViewById(R.id.etMilkRate);
        EditText etDefaultQuantity = view.findViewById(R.id.etDefaultQuantity);

        etMilkRate.setText(String.valueOf(milkRate));
        etDefaultQuantity.setText(String.valueOf(defaultQuantity));

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    milkRate = parseDouble(etMilkRate.getText().toString(), milkRate);
                    defaultQuantity = parseDouble(etDefaultQuantity.getText().toString(), defaultQuantity);
                    saveSettings();
                    Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear all data")
                .setMessage("This will delete all milk entries. Continue?")
                .setPositiveButton("Delete", (d, which) -> {
                    dbExecutor.execute(() -> {
                        milkEntryDao.deleteAll();
                        runOnUiThread(() -> {
                            loadMonthData();
                            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        milkRate = Double.longBitsToDouble(prefs.getLong(PREF_RATE, Double.doubleToLongBits(50.0)));
        defaultQuantity = Double.longBitsToDouble(prefs.getLong(PREF_DEFAULT_QTY, Double.doubleToLongBits(0.5)));
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(PREF_RATE, Double.doubleToLongBits(milkRate))
                .putLong(PREF_DEFAULT_QTY, Double.doubleToLongBits(defaultQuantity))
                .apply();
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static class Summary {
        final int daysTaken;
        final String totalLiters;
        final String totalAmount;

        Summary(int daysTaken, double liters, double amount) {
            this.daysTaken = daysTaken;
            this.totalLiters = String.format(Locale.getDefault(), "%.1f L", liters);
            this.totalAmount = String.format(Locale.getDefault(), "Rs %.0f", amount);
        }
    }
}
