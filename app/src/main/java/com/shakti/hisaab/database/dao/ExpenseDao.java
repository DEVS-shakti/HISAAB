package com.shakti.hisaab.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shakti.hisaab.database.entities.Expense;
import com.shakti.hisaab.model.CategoryTotal;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Query("DELETE FROM expenses WHERE id = :id")
    void delete(int id);

    @Query("DELETE FROM expenses")
    void deleteAll();

    @Query("DELETE FROM expenses WHERE category = :category")
    void deleteByCategory(String category);

    @Query("UPDATE expenses SET category = :newCategory WHERE category = :oldCategory")
    void renameCategory(String oldCategory, String newCategory);

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY dateMillis DESC")
    LiveData<List<Expense>> getExpensesByCategory(String category);

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :startMillis AND :endMillis ORDER BY dateMillis DESC")
    LiveData<List<Expense>> getExpensesBetween(long startMillis, long endMillis);

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC LIMIT :limit")
    LiveData<List<Expense>> getRecentExpenses(int limit);

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    List<Expense> getAllExpensesSync();

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category")
    LiveData<Double> getSumByCategory(String category);

    @Query("SELECT COUNT(*) FROM expenses WHERE category = :category AND isPaid = 0")
    LiveData<Integer> getUnpaidCountByCategory(String category);

    @Query("SELECT * FROM expenses WHERE isPaid = 0")
    List<Expense> getUnpaidExpenses();

    @Query("SELECT SUM(amount) FROM expenses")
    LiveData<Double> getTotalSum();

    @Query("SELECT SUM(amount) FROM expenses WHERE isPaid = 0")
    LiveData<Double> getTotalUnpaidSum();

    @Query("SELECT category, SUM(amount) AS total FROM expenses " +
            "WHERE dateMillis BETWEEN :startMillis AND :endMillis " +
            "GROUP BY category")
    LiveData<List<CategoryTotal>> getCategoryTotalsForMonth(long startMillis, long endMillis);
}
