package com.shakti.hisaab;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.shakti.hisaab.adapters.CategoryAdapter;
import com.shakti.hisaab.adapters.RecentActivityAdapter;
import com.shakti.hisaab.backup.AppBackupManager;
import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.model.CategoryItem;
import com.shakti.hisaab.reminder.ReminderScheduler;
import com.shakti.hisaab.view.MiniTrendChartView;
import com.shakti.hisaab.viewmodel.ExpenseViewModel;

import java.text.DateFormatSymbols;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> backupLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::handleBackupUri);
    private final ActivityResultLauncher<String[]> restoreLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleRestoreUri);
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    ReminderScheduler.setMasterEnabled(this, true);
                    syncReminderSwitch(true);
                    ReminderScheduler.scheduleAll(this);
                    toast("Reminders enabled");
                } else {
                    ReminderScheduler.setMasterEnabled(this, false);
                    syncReminderSwitch(false);
                    toast("Notification permission is required for reminders.");
                }
            });

    private enum Screen {
        HOME, REPORTS, SETTINGS
    }

    private interface TimeSelectionCallback {
        void onTimeSelected(int hour, int minute);
    }

    private ExpenseViewModel viewModel;
    private SharedPreferences preferences;
    private ExecutorService ioExecutor;

    private View screenHome;
    private View screenReports;
    private NestedScrollView screenSettings;
    private TextView tvToolbarTitle;
    private TextView btnThemeToggle;
    private TextView tvGreetingText;
    private TextView tvHeroLabel;
    private TextView tvHeroAmount;
    private TextView tvUnpaidDues;
    private TextView tvCurrentMonth;
    private TextView tvReportTotal;
    private TextView tvSummaryPaid;
    private TextView tvSummaryPending;
    private TextView tvSummaryBills;
    private TextView tvSummaryTrend;
    private TextView tvCurrencySummary;
    private LinearLayout layoutBarChart;
    private LinearLayout layoutBreakdown;
    private MiniTrendChartView trendChartView;
    private MaterialSwitch switchReminders;
    private View btnOpenReminders;

    private View navHome;
    private View navReports;
    private View navSettings;
    private TextView tvNavHomeIcon;
    private TextView tvNavHomeLabel;
    private TextView tvNavReportsIcon;
    private TextView tvNavReportsLabel;
    private TextView tvNavSettingsIcon;
    private TextView tvNavSettingsLabel;

    private CategoryAdapter categoryAdapter;
    private final List<CategoryItem> categoryItems = new ArrayList<>();
    private RecentActivityAdapter recentActivityAdapter;
    private Screen currentScreen = Screen.HOME;
    private YearMonth selectedMonth = YearMonth.now();
    private LiveData<List<Expense>> selectedMonthExpensesLiveData;
    private LiveData<List<Expense>> trendExpensesLiveData;
    private final Observer<List<Expense>> selectedMonthObserver = this::renderSelectedMonth;
    private final Observer<List<Expense>> trendObserver = this::renderTrend;
    private final Map<YearMonth, Double> trendTotals = new LinkedHashMap<>();
    private boolean updatingReminderSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = AppPreferences.getUiPreferences(this);
        applySavedTheme();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ioExecutor = Executors.newSingleThreadExecutor();
        ReminderScheduler.ensureDefaults(this);
        ReminderScheduler.ensureNotificationChannel(this);
        if (ReminderScheduler.isMasterEnabled(this)) {
            ReminderScheduler.scheduleAll(this);
        }

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        bindViews();
        setupToolbar();
        setupHomeScreen();
        setupReportsScreen();
        setupSettingsScreen();
        observeStaticData();
        observeSelectedMonth();
        observeTrendData();
        updateGreeting();
        refreshPreferenceDrivenUi();
        switchScreen(Screen.HOME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPreferenceDrivenUi();
    }

    private void bindViews() {
        screenHome = findViewById(R.id.screenHome);
        screenReports = findViewById(R.id.screenReports);
        screenSettings = findViewById(R.id.screenSettings);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        tvGreetingText = findViewById(R.id.tvGreetingText);
        tvHeroLabel = findViewById(R.id.tvHeroLabel);
        tvHeroAmount = findViewById(R.id.tvHeroAmount);
        tvUnpaidDues = findViewById(R.id.tvUnpaidDues);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvReportTotal = findViewById(R.id.tvReportTotal);
        tvSummaryPaid = findViewById(R.id.tvSummaryPaid);
        tvSummaryPending = findViewById(R.id.tvSummaryPending);
        tvSummaryBills = findViewById(R.id.tvSummaryBills);
        tvSummaryTrend = findViewById(R.id.tvSummaryTrend);
        tvCurrencySummary = findViewById(R.id.tvCurrencySummary);
        layoutBarChart = findViewById(R.id.layoutBarChart);
        layoutBreakdown = findViewById(R.id.layoutBreakdown);
        trendChartView = findViewById(R.id.trendChartView);
        switchReminders = findViewById(R.id.switchReminders);
        btnOpenReminders = findViewById(R.id.btnOpenReminders);

        navHome = findViewById(R.id.navHome);
        navReports = findViewById(R.id.navReports);
        navSettings = findViewById(R.id.navSettings);
        tvNavHomeIcon = findViewById(R.id.tvNavHomeIcon);
        tvNavHomeLabel = findViewById(R.id.tvNavHomeLabel);
        tvNavReportsIcon = findViewById(R.id.tvNavReportsIcon);
        tvNavReportsLabel = findViewById(R.id.tvNavReportsLabel);
        tvNavSettingsIcon = findViewById(R.id.tvNavSettingsIcon);
        tvNavSettingsLabel = findViewById(R.id.tvNavSettingsLabel);
    }

    private void setupToolbar() {
        findViewById(R.id.btnToolbarBack).setOnClickListener(v -> {
            if (currentScreen == Screen.HOME) {
                finish();
            } else {
                switchScreen(Screen.HOME);
            }
        });

        btnThemeToggle.setOnClickListener(v -> toggleTheme());
        updateThemeToggleIcon();
    }

    private void setupHomeScreen() {
        RecyclerView rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 2));
        categoryAdapter = new CategoryAdapter(categoryItems, this::openCategory);
        rvCategories.setAdapter(categoryAdapter);

        RecyclerView rvRecentActivity = findViewById(R.id.rvRecentActivity);
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(this::openRecentExpense);
        rvRecentActivity.setAdapter(recentActivityAdapter);

        findViewById(R.id.tvSeeReports).setOnClickListener(v -> switchScreen(Screen.REPORTS));
        findViewById(R.id.btnProfile).setOnClickListener(v -> switchScreen(Screen.SETTINGS));
        findViewById(R.id.tvEditCategories).setOnClickListener(v -> openCategorySettingsShortcut());

        navHome.setOnClickListener(v -> switchScreen(Screen.HOME));
        navReports.setOnClickListener(v -> switchScreen(Screen.REPORTS));
        navSettings.setOnClickListener(v -> switchScreen(Screen.SETTINGS));
    }

    private void setupReportsScreen() {
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            selectedMonth = selectedMonth.minusMonths(1);
            observeSelectedMonth();
            observeTrendData();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            selectedMonth = selectedMonth.plusMonths(1);
            observeSelectedMonth();
            observeTrendData();
        });
    }

    private void setupSettingsScreen() {
        syncReminderSwitch(ReminderScheduler.isMasterEnabled(this));
        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingReminderSwitch) {
                return;
            }

            if (isChecked && needsNotificationPermission()) {
                syncReminderSwitch(false);
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }

            ReminderScheduler.setMasterEnabled(this, isChecked);
            if (isChecked) {
                ReminderScheduler.scheduleAll(this);
            } else {
                ReminderScheduler.cancelAll(this);
            }
            toast(isChecked ? "Reminders enabled" : "Reminders disabled");
        });

        findViewById(R.id.itemReminderCenter).setOnClickListener(v -> openReminderSettingsDialog());
        btnOpenReminders.setOnClickListener(v -> openReminderSettingsDialog());
        findViewById(R.id.itemCurrency).setOnClickListener(v -> openCurrencyDialog());
        findViewById(R.id.itemManageCategories).setOnClickListener(v -> openManageCategoriesDialog());
        findViewById(R.id.btnBackupSmall).setOnClickListener(v -> launchBackupFlow());
        findViewById(R.id.btnBackupNow).setOnClickListener(v -> launchBackupFlow());
        findViewById(R.id.btnRestore).setOnClickListener(v -> launchRestoreFlow());

        View.OnClickListener clearListener = v -> confirmClearAll();
        findViewById(R.id.btnClearSmall).setOnClickListener(clearListener);
    }

    private void observeStaticData() {
        viewModel.getRecentExpenses(5).observe(this, recentActivityAdapter::setItems);
        viewModel.getTotalUnpaidSum().observe(this, total -> {
            double unpaid = total == null ? 0 : total;
            tvUnpaidDues.setText(AppPreferences.formatAmount(this, unpaid) + " Unpaid Dues");
        });
    }

    private void observeSelectedMonth() {
        if (selectedMonthExpensesLiveData != null) {
            selectedMonthExpensesLiveData.removeObserver(selectedMonthObserver);
        }
        long start = getMonthStart(selectedMonth);
        long end = getMonthEnd(selectedMonth);
        selectedMonthExpensesLiveData = viewModel.getExpensesBetween(start, end);
        selectedMonthExpensesLiveData.observe(this, selectedMonthObserver);
        tvCurrentMonth.setText(formatMonth(selectedMonth));
        tvHeroLabel.setText("Total Spend (" + selectedMonth.getMonth().name().substring(0, 1)
                + selectedMonth.getMonth().name().substring(1).toLowerCase(Locale.getDefault()) + ")");
    }

    private void observeTrendData() {
        if (trendExpensesLiveData != null) {
            trendExpensesLiveData.removeObserver(trendObserver);
        }
        YearMonth startMonth = selectedMonth.minusMonths(4);
        long start = getMonthStart(startMonth);
        long end = getMonthEnd(selectedMonth);
        trendExpensesLiveData = viewModel.getExpensesBetween(start, end);
        trendExpensesLiveData.observe(this, trendObserver);
    }

    private void refreshPreferenceDrivenUi() {
        refreshCategoryCards();
        tvCurrencySummary.setText(AppPreferences.getCurrencyLabel(this));
        if (recentActivityAdapter != null) {
            recentActivityAdapter.refresh();
        }
        observeSelectedMonth();
        observeTrendData();
    }

    private void refreshCategoryCards() {
        categoryItems.clear();
        for (String category : AppPreferences.getCategories(this)) {
            categoryItems.add(new CategoryItem(
                    category,
                    CategoryDisplayHelper.getSubtitle(category),
                    CategoryDisplayHelper.getIcon(category),
                    CategoryDisplayHelper.getColorRes(category),
                    CategoryDisplayHelper.getBackgroundRes(category)
            ));
        }
        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
    }

    private void renderSelectedMonth(List<Expense> expenses) {
        List<Expense> safeExpenses = expenses == null ? Collections.emptyList() : expenses;
        double total = 0;
        double paid = 0;
        double pending = 0;
        for (Expense expense : safeExpenses) {
            total += expense.amount;
            if (expense.isPaid) {
                paid += expense.amount;
            } else {
                pending += expense.amount;
            }
        }

        tvHeroAmount.setText(AppPreferences.formatAmount(this, total));
        tvReportTotal.setText(AppPreferences.formatAmount(this, total));
        tvSummaryPaid.setText("Paid\n" + AppPreferences.formatAmount(this, paid));
        tvSummaryPending.setText("Pending\n" + AppPreferences.formatAmount(this, pending));
        tvSummaryBills.setText(String.format(Locale.getDefault(), "Bills Paid\n%d", safeExpenses.size()));

        renderBreakdown(safeExpenses, total);
        updateTrendSummary(total);
    }

    private void renderTrend(List<Expense> expenses) {
        trendTotals.clear();
        YearMonth startMonth = selectedMonth.minusMonths(4);
        for (int i = 0; i < 5; i++) {
            trendTotals.put(startMonth.plusMonths(i), 0d);
        }
        if (expenses != null) {
            for (Expense expense : expenses) {
                YearMonth month = getYearMonth(expense.dateMillis);
                if (trendTotals.containsKey(month)) {
                    trendTotals.put(month, trendTotals.get(month) + expense.amount);
                }
            }
        }

        List<Float> chartPoints = new ArrayList<>();
        for (Double total : trendTotals.values()) {
            chartPoints.add(total.floatValue());
        }
        trendChartView.setPoints(chartPoints);
        renderBarChart();
        updateTrendSummary(trendTotals.get(selectedMonth) == null ? 0 : trendTotals.get(selectedMonth));
    }

    private void renderBarChart() {
        layoutBarChart.removeAllViews();
        double max = 1;
        for (Double value : trendTotals.values()) {
            if (value > max) {
                max = value;
            }
        }

        for (Map.Entry<YearMonth, Double> entry : trendTotals.entrySet()) {
            LinearLayout barColumn = new LinearLayout(this);
            barColumn.setOrientation(LinearLayout.VERTICAL);
            barColumn.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams columnParams =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            columnParams.setMargins(6, 0, 6, 0);
            barColumn.setLayoutParams(columnParams);

            TextView valueView = new TextView(this);
            valueView.setText(String.format(Locale.getDefault(), "%.0fK", entry.getValue() / 1000d));
            valueView.setTextSize(12);
            valueView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            valueView.setPadding(0, 0, 0, 6);

            View bar = new View(this);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.max((int) ((entry.getValue() / max) * dp(120)), (int) dp(18))
            );
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.bg_hero_gradient);
            bar.setBackground(drawable);
            bar.setLayoutParams(barParams);
            bar.setOnClickListener(v -> toast(getShortMonth(entry.getKey()) + ": "
                    + AppPreferences.formatAmount(this, entry.getValue())));

            TextView labelView = new TextView(this);
            labelView.setText(getShortMonth(entry.getKey()));
            labelView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            labelView.setTextSize(12);
            labelView.setPadding(0, 8, 0, 0);

            barColumn.addView(valueView);
            barColumn.addView(bar);
            barColumn.addView(labelView);
            layoutBarChart.addView(barColumn);
        }
    }

    private void renderBreakdown(List<Expense> expenses, double total) {
        layoutBreakdown.removeAllViews();
        if (expenses.isEmpty() || total <= 0) {
            return;
        }

        Map<String, Double> byCategory = new HashMap<>();
        for (Expense expense : expenses) {
            double current = byCategory.containsKey(expense.category) ? byCategory.get(expense.category) : 0;
            byCategory.put(expense.category, current + expense.amount);
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(byCategory.entrySet());
        entries.sort((first, second) -> Double.compare(second.getValue(), first.getValue()));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : entries) {
            View row = inflater.inflate(R.layout.item_breakdown, layoutBreakdown, false);
            TextView tvCategory = row.findViewById(R.id.tvBreakdownCategory);
            TextView tvAmount = row.findViewById(R.id.tvBreakdownAmount);
            ProgressBar progressBar = row.findViewById(R.id.progressBreakdown);

            int percent = (int) Math.round((entry.getValue() / total) * 100);
            tvCategory.setText(CategoryDisplayHelper.getIcon(entry.getKey()) + "  " + entry.getKey());
            tvAmount.setText(AppPreferences.formatAmount(this, entry.getValue()) + " (" + percent + "%)");
            progressBar.setProgress(percent);
            progressBar.setProgressTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, CategoryDisplayHelper.getColorRes(entry.getKey())))
            );
            layoutBreakdown.addView(row);
        }
    }

    private void updateTrendSummary(double selectedMonthTotal) {
        Double previousTotal = trendTotals.get(selectedMonth.minusMonths(1));
        double prev = previousTotal == null ? 0 : previousTotal;
        if (prev <= 0) {
            tvSummaryTrend.setText("Trend\nNew Month");
            return;
        }
        double delta = ((selectedMonthTotal - prev) / prev) * 100d;
        tvSummaryTrend.setText(String.format(Locale.getDefault(), "Trend\n%+.0f%%", delta));
    }

    private void openCategory(CategoryItem item) {
        if ("Milk".equalsIgnoreCase(item.name)) {
            startActivity(new Intent(this, MilkCalendarActivity.class));
            return;
        }
        Intent intent = new Intent(this, CategoryDetailActivity.class);
        intent.putExtra(CategoryDetailActivity.EXTRA_CATEGORY, item.name);
        startActivity(intent);
    }

    private void openRecentExpense(Expense expense) {
        if ("Milk".equalsIgnoreCase(expense.category)) {
            startActivity(new Intent(this, MilkCalendarActivity.class));
            return;
        }
        Intent intent = new Intent(this, CategoryDetailActivity.class);
        intent.putExtra(CategoryDetailActivity.EXTRA_CATEGORY, expense.category);
        startActivity(intent);
    }

    private void openCategorySettingsShortcut() {
        switchScreen(Screen.SETTINGS);
        View target = findViewById(R.id.itemManageCategories);
        screenSettings.post(() -> screenSettings.smoothScrollTo(0, target.getTop()));
    }

    private void openCurrencyDialog() {
        String[] labels = AppPreferences.getCurrencyLabels();
        String[] codes = AppPreferences.getCurrencyCodes();
        String currentCode = AppPreferences.getCurrencyCode(this);
        int selectedIndex = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentCode)) {
                selectedIndex = i;
                break;
            }
        }

        final int[] selected = {selectedIndex};
        new AlertDialog.Builder(this)
                .setTitle("Select currency")
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Save", (dialog, which) -> {
                    AppPreferences.setCurrencyCode(this, codes[selected[0]]);
                    refreshPreferenceDrivenUi();
                    toast("Currency updated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openManageCategoriesDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_categories, null);
        dialog.setContentView(view);

        LinearLayout layoutCategoryList = view.findViewById(R.id.layoutCategoryList);
        MaterialButton btnAddCategory = view.findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> openCategoryEditorDialog(null, dialog, layoutCategoryList));

        renderCategoryManagerRows(layoutCategoryList, dialog);
        dialog.show();
    }

    private void renderCategoryManagerRows(LinearLayout container, BottomSheetDialog parentDialog) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String category : AppPreferences.getCategories(this)) {
            View row = inflater.inflate(R.layout.item_manage_category, container, false);
            TextView tvCategoryBadge = row.findViewById(R.id.tvCategoryBadge);
            TextView tvCategoryName = row.findViewById(R.id.tvCategoryName);
            TextView tvCategoryHint = row.findViewById(R.id.tvCategoryHint);

            tvCategoryBadge.setText(CategoryDisplayHelper.getIcon(category));
            tvCategoryBadge.setTextColor(ContextCompat.getColor(this, CategoryDisplayHelper.getColorRes(category)));
            tvCategoryBadge.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, CategoryDisplayHelper.getBackgroundRes(category))
            );
            tvCategoryName.setText(category);
            tvCategoryHint.setText(AppPreferences.isReservedCategory(category)
                    ? "Built-in category"
                    : "Tap to edit or delete");

            row.setOnClickListener(v -> {
                if (AppPreferences.isReservedCategory(category)) {
                    toast("Milk category is fixed and cannot be changed.");
                    return;
                }
                openCategoryEditorDialog(category, parentDialog, container);
            });
            container.addView(row);
        }
    }

    private void openCategoryEditorDialog(String existingCategory, BottomSheetDialog parentDialog,
                                          LinearLayout listContainer) {
        boolean isEditing = existingCategory != null;
        EditText input = new EditText(this);
        input.setHint("Category name");
        input.setText(isEditing ? existingCategory : "");
        input.setSelection(input.getText().length());
        int padding = dp(20);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEditing ? "Edit category" : "Add category")
                .setView(input)
                .setPositiveButton(isEditing ? "Save" : "Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (isEditing) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Delete", (d, which) -> {
            });
        }

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = AppPreferences.normalizeCategoryName(input.getText().toString());
                if (newName.isEmpty()) {
                    input.setError("Enter a category name");
                    return;
                }

                List<String> categories = AppPreferences.getCategories(this);
                for (String category : categories) {
                    if (category.equalsIgnoreCase(newName)
                            && (existingCategory == null || !category.equalsIgnoreCase(existingCategory))) {
                        input.setError("Category already exists");
                        return;
                    }
                }

                if (existingCategory == null) {
                    categories.add(newName);
                    AppPreferences.saveCategories(this, categories);
                    toast("Category added");
                } else {
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).equalsIgnoreCase(existingCategory)) {
                            categories.set(i, newName);
                            break;
                        }
                    }
                    AppPreferences.saveCategories(this, categories);
                    if (!existingCategory.equalsIgnoreCase(newName)) {
                        viewModel.renameCategory(existingCategory, newName);
                    }
                    toast("Category updated");
                }

                refreshPreferenceDrivenUi();
                renderCategoryManagerRows(listContainer, parentDialog);
                dialog.dismiss();
            });

            if (isEditing) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> confirmDeleteCategory(
                        existingCategory, dialog, parentDialog, listContainer));
            }
        });
        dialog.show();
    }

    private void confirmDeleteCategory(String category, AlertDialog editorDialog,
                                       BottomSheetDialog parentDialog, LinearLayout listContainer) {
        new AlertDialog.Builder(this)
                .setTitle("Delete category")
                .setMessage("Delete " + category + " and all its entries?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<String> categories = AppPreferences.getCategories(this);
                    List<String> updated = new ArrayList<>();
                    for (String item : categories) {
                        if (!item.equalsIgnoreCase(category)) {
                            updated.add(item);
                        }
                    }
                    AppPreferences.saveCategories(this, updated);
                    viewModel.deleteByCategory(category);
                    refreshPreferenceDrivenUi();
                    renderCategoryManagerRows(listContainer, parentDialog);
                    editorDialog.dismiss();
                    toast("Category deleted");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchScreen(Screen screen) {
        currentScreen = screen;
        screenHome.setVisibility(screen == Screen.HOME ? View.VISIBLE : View.GONE);
        screenReports.setVisibility(screen == Screen.REPORTS ? View.VISIBLE : View.GONE);
        screenSettings.setVisibility(screen == Screen.SETTINGS ? View.VISIBLE : View.GONE);

        if (screen == Screen.HOME) {
            tvToolbarTitle.setText("Dashboard");
        } else if (screen == Screen.REPORTS) {
            tvToolbarTitle.setText("Analytics");
        } else {
            tvToolbarTitle.setText("Settings");
        }
        updateNavState();
    }

    private void updateNavState() {
        setNavColors(tvNavHomeIcon, tvNavHomeLabel, currentScreen == Screen.HOME);
        setNavColors(tvNavReportsIcon, tvNavReportsLabel, currentScreen == Screen.REPORTS);
        setNavColors(tvNavSettingsIcon, tvNavSettingsLabel, currentScreen == Screen.SETTINGS);
    }

    private void setNavColors(TextView iconView, TextView labelView, boolean active) {
        int color = ContextCompat.getColor(this, active ? R.color.primary : R.color.text_muted);
        iconView.setTextColor(color);
        labelView.setTextColor(color);
    }

    private void toggleTheme() {
        boolean isDark = isDarkMode();
        boolean targetDark = !isDark;
        preferences.edit().putBoolean(AppPreferences.PREF_DARK_MODE, targetDark).apply();
        AppCompatDelegate.setDefaultNightMode(
                targetDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        updateThemeToggleIcon();
    }

    private void applySavedTheme() {
        boolean dark = preferences.getBoolean(AppPreferences.PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void updateThemeToggleIcon() {
        btnThemeToggle.setText(isDarkMode() ? "\u2600" : "\uD83C\uDF13");
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear all data")
                .setMessage("Delete all generic expense entries?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    viewModel.deleteAll();
                    toast("All generic expenses cleared.");
                    observeSelectedMonth();
                    observeTrendData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openReminderSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reminder_settings, null);
        MaterialSwitch switchMilkReminder = view.findViewById(R.id.switchMilkReminder);
        MaterialSwitch switchRentReminder = view.findViewById(R.id.switchRentReminder);
        MaterialSwitch switchCustomReminder = view.findViewById(R.id.switchCustomReminder);
        EditText etRentDay = view.findViewById(R.id.etRentDay);
        EditText etCustomReminderTitle = view.findViewById(R.id.etCustomReminderTitle);
        EditText etCustomReminderMessage = view.findViewById(R.id.etCustomReminderMessage);
        MaterialButton btnMilkTime = view.findViewById(R.id.btnMilkTime);
        MaterialButton btnRentTime = view.findViewById(R.id.btnRentTime);
        MaterialButton btnCustomTime = view.findViewById(R.id.btnCustomTime);
        View btnCancel = view.findViewById(R.id.btnCancelReminderSettings);
        View btnSave = view.findViewById(R.id.btnSaveReminderSettings);

        SharedPreferences prefs = ReminderScheduler.getPrefs(this);
        final int[] milkHour = {prefs.getInt(ReminderScheduler.KEY_MILK_HOUR, 7)};
        final int[] milkMinute = {prefs.getInt(ReminderScheduler.KEY_MILK_MINUTE, 0)};
        final int[] rentHour = {prefs.getInt(ReminderScheduler.KEY_RENT_HOUR, 9)};
        final int[] rentMinute = {prefs.getInt(ReminderScheduler.KEY_RENT_MINUTE, 0)};
        final int[] customHour = {prefs.getInt(ReminderScheduler.KEY_CUSTOM_HOUR, 20)};
        final int[] customMinute = {prefs.getInt(ReminderScheduler.KEY_CUSTOM_MINUTE, 0)};

        switchMilkReminder.setChecked(prefs.getBoolean(ReminderScheduler.KEY_MILK_ENABLED, true));
        switchRentReminder.setChecked(prefs.getBoolean(ReminderScheduler.KEY_RENT_ENABLED, true));
        switchCustomReminder.setChecked(prefs.getBoolean(ReminderScheduler.KEY_CUSTOM_ENABLED, false));
        etRentDay.setText(String.valueOf(prefs.getInt(ReminderScheduler.KEY_RENT_DAY, 1)));
        etCustomReminderTitle.setText(prefs.getString(ReminderScheduler.KEY_CUSTOM_TITLE, "Custom Reminder"));
        etCustomReminderMessage.setText(prefs.getString(ReminderScheduler.KEY_CUSTOM_MESSAGE, "Check today's Hisaab tasks."));
        btnMilkTime.setText(formatTime(milkHour[0], milkMinute[0]));
        btnRentTime.setText(formatTime(rentHour[0], rentMinute[0]));
        btnCustomTime.setText(formatTime(customHour[0], customMinute[0]));

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        btnMilkTime.setOnClickListener(v -> openTimePicker(milkHour[0], milkMinute[0], (hour, minute) -> {
            milkHour[0] = hour;
            milkMinute[0] = minute;
            btnMilkTime.setText(formatTime(hour, minute));
        }));

        btnRentTime.setOnClickListener(v -> openTimePicker(rentHour[0], rentMinute[0], (hour, minute) -> {
            rentHour[0] = hour;
            rentMinute[0] = minute;
            btnRentTime.setText(formatTime(hour, minute));
        }));

        btnCustomTime.setOnClickListener(v -> openTimePicker(customHour[0], customMinute[0], (hour, minute) -> {
            customHour[0] = hour;
            customMinute[0] = minute;
            btnCustomTime.setText(formatTime(hour, minute));
        }));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            int rentDay = clamp(parseInt(etRentDay.getText().toString(), 1), 1, 28);
            String customTitle = getNonEmptyText(etCustomReminderTitle, "Custom Reminder");
            String customMessage = getNonEmptyText(etCustomReminderMessage, "Check today's Hisaab tasks.");

            prefs.edit()
                    .putBoolean(ReminderScheduler.KEY_MILK_ENABLED, switchMilkReminder.isChecked())
                    .putInt(ReminderScheduler.KEY_MILK_HOUR, milkHour[0])
                    .putInt(ReminderScheduler.KEY_MILK_MINUTE, milkMinute[0])
                    .putBoolean(ReminderScheduler.KEY_RENT_ENABLED, switchRentReminder.isChecked())
                    .putInt(ReminderScheduler.KEY_RENT_DAY, rentDay)
                    .putInt(ReminderScheduler.KEY_RENT_HOUR, rentHour[0])
                    .putInt(ReminderScheduler.KEY_RENT_MINUTE, rentMinute[0])
                    .putBoolean(ReminderScheduler.KEY_CUSTOM_ENABLED, switchCustomReminder.isChecked())
                    .putString(ReminderScheduler.KEY_CUSTOM_TITLE, customTitle)
                    .putString(ReminderScheduler.KEY_CUSTOM_MESSAGE, customMessage)
                    .putInt(ReminderScheduler.KEY_CUSTOM_HOUR, customHour[0])
                    .putInt(ReminderScheduler.KEY_CUSTOM_MINUTE, customMinute[0])
                    .apply();

            if (ReminderScheduler.isMasterEnabled(this)) {
                ReminderScheduler.scheduleAll(this);
                toast("Reminder settings saved");
            } else {
                ReminderScheduler.cancelAll(this);
                toast("Saved. Turn on App Reminders to activate them.");
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void launchBackupFlow() {
        backupLauncher.launch("hisaab_backup_" + System.currentTimeMillis() + ".json");
    }

    private void launchRestoreFlow() {
        restoreLauncher.launch(new String[]{"application/json", "text/*"});
    }

    private void handleBackupUri(Uri uri) {
        if (uri == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                AppBackupManager.exportBackup(this, uri);
                runOnUiThread(() -> toast("Backup saved successfully."));
            } catch (Exception e) {
                runOnUiThread(() -> toast("Backup failed: " + e.getMessage()));
            }
        });
    }

    private void handleRestoreUri(Uri uri) {
        if (uri == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Restore backup")
                .setMessage("This will replace current app data with the selected backup file.")
                .setPositiveButton("Restore", (dialog, which) -> ioExecutor.execute(() -> {
                    try {
                        AppBackupManager.restoreBackup(this, uri);
                        ReminderScheduler.ensureDefaults(this);
                        if (ReminderScheduler.isMasterEnabled(this)) {
                            ReminderScheduler.scheduleAll(this);
                        } else {
                            ReminderScheduler.cancelAll(this);
                        }
                        runOnUiThread(() -> {
                            toast("Backup restored.");
                            recreate();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> toast("Restore failed: " + e.getMessage()));
                    }
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void syncReminderSwitch(boolean enabled) {
        updatingReminderSwitch = true;
        switchReminders.setChecked(enabled);
        updatingReminderSwitch = false;
    }

    private void openTimePicker(int hour, int minute, TimeSelectionCallback callback) {
        new TimePickerDialog(this, (view, selectedHour, selectedMinute) ->
                callback.onTimeSelected(selectedHour, selectedMinute), hour, minute, false).show();
    }

    private String formatTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return String.format(Locale.getDefault(), "%1$tl:%1$tM %1$Tp", calendar).toUpperCase(Locale.getDefault());
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String getNonEmptyText(EditText editText, String fallback) {
        String value = editText.getText() == null ? "" : editText.getText().toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvGreetingText.setText("Good Morning");
        } else if (hour < 17) {
            tvGreetingText.setText("Good Afternoon");
        } else if (hour < 21) {
            tvGreetingText.setText("Good Evening");
        } else {
            tvGreetingText.setText("Good Night");
        }
    }

    private long getMonthStart(YearMonth month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, month.getYear());
        calendar.set(Calendar.MONTH, month.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getMonthEnd(YearMonth month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getMonthStart(month));
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTimeInMillis();
    }

    private YearMonth getYearMonth(long dateMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        return YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }

    private String formatMonth(YearMonth month) {
        String monthName = new DateFormatSymbols().getMonths()[month.getMonthValue() - 1];
        return monthName + " " + month.getYear();
    }

    private String getShortMonth(YearMonth month) {
        return new DateFormatSymbols().getShortMonths()[month.getMonthValue() - 1];
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (selectedMonthExpensesLiveData != null) {
            selectedMonthExpensesLiveData.removeObserver(selectedMonthObserver);
        }
        if (trendExpensesLiveData != null) {
            trendExpensesLiveData.removeObserver(trendObserver);
        }
        if (ioExecutor != null) {
            ioExecutor.shutdown();
        }
    }
}
