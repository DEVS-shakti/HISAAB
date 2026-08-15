# Implementation Plan - Monthly Budget System

Implement a production-quality monthly budget system integrated into HISAAB.

## User Review Required

> [!IMPORTANT]
> - Database schema will be updated to version 5. `fallbackToDestructiveMigration()` is currently enabled in `AppDatabase`, which might wipe data if a manual migration isn't provided. I will implement a safe migration.
> - The backup system will be migrated from raw SQLite file copy to a versioned JSON format (`hisaab_backup_v3.json`).
> - New activities and UI components will be added following the existing design system.

## Proposed Changes

### Data Layer

#### [MODIFY] [Budget.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/database/entities/Budget.java)
- Add `year` field.
- Add `createdAt` field.

#### [NEW] [BudgetRevision.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/database/entities/BudgetRevision.java)
- Store history of budget changes (Old amount, New amount, Timestamp).

#### [MODIFY] [BudgetDao.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/database/dao/BudgetDao.java)
- Add methods to get all budgets (for history).
- Add methods for `BudgetRevision`.

#### [MODIFY] [AppDatabase.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/database/AppDatabase.java)
- Add `BudgetRevision` entity.
- Bump version to 5.
- Add Migration logic.

#### [NEW] [BudgetViewModel.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/viewmodel/BudgetViewModel.java)
- Handle budget-related logic and insights calculations (safe daily spend, projection, etc.).

---

### UI Components

#### [MODIFY] [MainActivity.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/MainActivity.java)
- Integrate `BudgetViewModel`.
- Wire up the new Budget hero card in `HeroPagerAdapter`.
- Add swipe indicators (● ○).

#### [MODIFY] [HeroPagerAdapter.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/adapters/HeroPagerAdapter.java)
- Implement the Budget card UI with the requested hierarchy (Remaining amount primary).
- Implement the Over-budget state logic.

#### [NEW] [BudgetActivity.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/BudgetActivity.java)
- Detailed overview of current month's budget.
- Safe daily spend calculations.

#### [NEW] [BudgetHistoryActivity.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/BudgetHistoryActivity.java)
- List of previous months with Budget vs Actual comparison.

#### [NEW] [BudgetInsightsActivity.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/BudgetInsightsActivity.java)
- Detailed spending analysis and projections.

---

### Backup & Restore

#### [MODIFY] [AppBackupManager.java](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/src/main/java/com/shakti/hisaab/backup/AppBackupManager.java)
- Replace raw DB copy with JSON serialization.
- Include all entities and `SharedPreferences` (categories).

#### [MODIFY] [build.gradle](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/app/build.gradle) and [libs.versions.toml](file:///C:/Users/jaypr/AndroidStudioProjects/HISAAB/gradle/libs.versions.toml)
- Add `gson` library for reliable JSON handling.

## Verification Plan

### Automated Tests
- Unit tests for budget calculations (Safe daily spend, Projected spend).
- Verification of JSON backup structure.

### Manual Verification
1. Verify Dashboard swipe between Expense and Budget.
2. Verify "Manage Budget" opens `BudgetActivity`.
3. Set a budget and verify the hero card updates.
4. Add expenses to exceed budget and verify "OVER BUDGET" state.
5. Perform a backup, clear data, and restore to verify data persistence (Expenses + Budgets).
6. Verify "Budget History" shows correct previous months.
