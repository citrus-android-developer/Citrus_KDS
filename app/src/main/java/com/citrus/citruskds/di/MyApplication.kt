package com.citrus.citruskds.di

import android.app.Application
import com.citrus.citruskds.util.Prefs

import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject


val prefs: Prefs by lazy {
    MyApplication.prefs!!
}

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var mPrefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = mPrefs

        Timber.plant(MultiTagTree())
    }

    companion object {
        var prefs: Prefs? = null
    }
}