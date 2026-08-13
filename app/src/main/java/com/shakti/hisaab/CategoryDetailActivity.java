package com.shakti.hisaab;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shakti.hisaab.adapters.ExpenseAdapter;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.viewmodel.ExpenseViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CategoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "extra_category";

    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;
    private String category;
    private long selectedDateMillis;

    private TextView tvTotalSpent;
    private TextView tvUnpaidCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (TextUtils.isEmpty(category)) {
            category = "Expense";
        }

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        setupToolbar();
        setupSummary();
        setupList();
        setupFab();
    }

    private void setupToolbar() {
        TextView tvTitle = findViewById(R.id.tvToolbarTitle);
        ImageButton btnBack = findViewById(R.id.btnBack);
        View toolbarContainer = findViewById(R.id.toolbarContainer);

        int colorRes = CategoryDisplayHelper.getColorRes(category);
        int color = ContextCompat.getColor(this, colorRes);
        toolbarContainer.setBackgroundColor(color);

        tvTitle.setText(category);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.color_card));
        btnBack.setColorFilter(ContextCompat.getColor(this, R.color.color_card));
        btnBack.setOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
    }

    private void setupSummary() {
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvUnpaidCount = findViewById(R.id.tvUnpaidCount);

        viewModel.getSumByCategory(category).observe(this, total -> {
            double safeTotal = total == null ? 0 : total;
            tvTotalSpent.setText(AppPreferences.formatAmount(this, safeTotal));
        });

        viewModel.getUnpaidCountByCategory(category).observe(this, count -> {
            int safeCount = count == null ? 0 : count;
            tvUnpaidCount.setText(String.valueOf(safeCount));
        });
    }

    private void setupList() {
        RecyclerView rvExpenses = findViewById(R.id.rvExpenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(this::openExpenseBottomSheet);
        rvExpenses.setAdapter(adapter);

        viewModel.getExpensesByCategory(category).observe(this, adapter::setItems);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> openExpenseBottomSheet(null));
    }

    private void openExpenseBottomSheet(Expense existingExpense) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_add_expense, null);
        dialog.setContentView(view);

        TextView tvSheetTitle = view.findViewById(R.id.tvSheetTitle);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etNote = view.findViewById(R.id.etNote);
        EditText etDate = view.findViewById(R.id.etDate);
        RadioButton rbPaid = view.findViewById(R.id.rbPaid);
        RadioButton rbUnpaid = view.findViewById(R.id.rbUnpaid);
        MaterialButton btnDelete = view.findViewById(R.id.btnDelete);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        boolean isEditing = existingExpense != null;
        tvSheetTitle.setText(isEditing ? "Edit Expense" : "Add Expense");
        btnSave.setText(isEditing ? "Save Changes" : "Save");
        btnDelete.setVisibility(isEditing ? View.VISIBLE : View.GONE);

        if (isEditing) {
            selectedDateMillis = existingExpense.dateMillis;
            etAmount.setText(String.format(Locale.getDefault(), "%.0f", existingExpense.amount));
            etNote.setText(existingExpense.note == null ? "" : existingExpense.note);
            rbPaid.setChecked(existingExpense.isPaid);
            rbUnpaid.setChecked(!existingExpense.isPaid);
        } else {
            selectedDateMillis = System.currentTimeMillis();
            rbPaid.setChecked(true);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(dateFormat.format(selectedDateMillis));
        etDate.setOnClickListener(v -> showDatePicker(etDate, dateFormat));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> confirmDelete(existingExpense, dialog));
        btnSave.setOnClickListener(v -> saveExpense(existingExpense, etAmount, etNote, rbPaid, dialog));

        dialog.show();
    }

    private void saveExpense(Expense existingExpense, EditText etAmount, EditText etNote,
                             RadioButton rbPaid, BottomSheetDialog dialog) {
        String amountText = etAmount.getText() == null ? "" : etAmount.getText().toString().trim();
        String noteText = etNote.getText() == null ? "" : etNote.getText().toString().trim();

        if (TextUtils.isEmpty(amountText)) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount should be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Expense expense = new Expense(category, amount, selectedDateMillis, noteText, rbPaid.isChecked());
        if (existingExpense != null) {
            expense.id = existingExpense.id;
            viewModel.update(expense);
            Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.insert(expense);
            Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
        }
        dialog.dismiss();
    }

    private void confirmDelete(Expense expense, BottomSheetDialog dialog) {
        if (expense == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete expense")
                .setMessage("Remove this item from " + category + "?")
                .setPositiveButton("Delete", (d, which) -> {
                    viewModel.delete(expense.id);
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker(EditText etDate, SimpleDateFormat dateFormat) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = picked.getTimeInMillis();
            etDate.setText(dateFormat.format(selectedDateMillis));
        }, year, month, day).show();
    }
}
