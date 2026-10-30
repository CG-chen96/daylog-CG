package com.dailybook.app

import android.app.Application
import com.dailybook.app.data.AppDatabase

class DaylogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
}
