package com.example.deutschlernen

import android.app.Application
import com.example.deutschlernen.data.DeutschRepository

class DeutschApplication : Application() {
    val repository: DeutschRepository by lazy { DeutschRepository(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
