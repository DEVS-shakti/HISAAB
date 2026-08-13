package com.shakti.hisaab.database.repository;

import android.content.Context;

import com.shakti.hisaab.database.AppDatabase;
import com.shakti.hisaab.database.dao.MilkEntryDao;
import com.shakti.hisaab.database.entities.MilkEntry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MilkRepository {
    private MilkEntryDao milkEntryDao;
    private ExecutorService executorService;

    public MilkRepository(Context context) {

        AppDatabase db = AppDatabase.getInstance(context);
        milkEntryDao = db.milkEntryDao();

        executorService = Executors.newSingleThreadExecutor();
    }
    //insert
    public void insert(MilkEntry entry)
    {
        executorService.execute(()->{
            milkEntryDao.insert(entry);
        });
    }
    //update
    public void update(MilkEntry entry)
    {
        executorService.execute(()->{
            milkEntryDao.update(entry);
        });
    }

    public void deleteByDate(String date) {
        executorService.execute(() -> milkEntryDao.deleteByDate(date));
    }

    public void deleteAll() {
        executorService.execute(milkEntryDao::deleteAll);
    }
}
