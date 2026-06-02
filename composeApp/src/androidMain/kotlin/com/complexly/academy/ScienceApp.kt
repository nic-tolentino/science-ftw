package com.complexly.academy

import android.app.Application
import com.complexly.academy.di.AppComponent
import com.complexly.academy.di.create

class ScienceApp : Application() {
    companion object {
        lateinit var instance: ScienceApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppComponent.init { AppComponent::class.create() }
    }
}
