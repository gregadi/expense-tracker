package com.gregoryadi.expenses

import android.app.Application

class ExpensesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedIfEmpty()
    }
}
