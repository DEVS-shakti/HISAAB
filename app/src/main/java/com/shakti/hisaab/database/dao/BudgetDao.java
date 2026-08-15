package com.shakti.hisaab.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shakti.hisaab.database.entities.Budget;
import com.shakti.hisaab.database.entities.BudgetRevision;

import java.util.List;

@Dao
public interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    LiveData<Budget> getBudgetForMonth(String month);

    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    Budget getBudgetForMonthSync(String month);

    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    LiveData<List<Budget>> getAllBudgets();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Insert
    void insertRevision(BudgetRevision revision);

    @Query("SELECT * FROM budget_revisions WHERE budgetId = :budgetId ORDER BY changedAt DESC")
    LiveData<List<BudgetRevision>> getRevisionsForBudget(int budgetId);

    @Query("SELECT * FROM budgets")
    List<Budget> getAllBudgetsSync();

    @Query("SELECT * FROM budget_revisions")
    List<BudgetRevision> getAllRevisionsSync();

    @Query("DELETE FROM budgets")
    void deleteAll();

    @Query("DELETE FROM budget_revisions")
    void deleteAllRevisions();
}
