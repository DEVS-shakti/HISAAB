package com.shakti.hisaab.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shakti.hisaab.database.entities.MilkEntry;

import java.util.List;

@Dao
public interface MilkEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MilkEntry milkEntry);

    @Update
    void update(MilkEntry milkEntry);

    @Query("SELECT * FROM milk_entries WHERE date = :date LIMIT 1")
    MilkEntry getEntryByDate(String date);

    @Query("SELECT * FROM milk_entries WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    LiveData<List<MilkEntry>> getEntriesForMonth(String monthPrefix);

    @Query("SELECT * FROM milk_entries WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    List<MilkEntry> getEntriesForMonthSync(String monthPrefix);

    @Query("SELECT * FROM milk_entries ORDER BY date ASC")
    List<MilkEntry> getAllEntries();

    @Query("DELETE FROM milk_entries WHERE date = :date")
    void deleteByDate(String date);

    @Query("DELETE FROM milk_entries")
    void deleteAll();

    @Query("SELECT SUM(totalCost) FROM milk_entries WHERE taken = 1")
    LiveData<Double> getTotalMilkSum();

    @Query("SELECT SUM(totalCost) FROM milk_entries WHERE taken = 1 AND paid = 0")
    LiveData<Double> getTotalUnpaidMilkSum();
}
