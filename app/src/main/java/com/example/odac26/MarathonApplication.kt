package com.example.odac26

import android.app.Application
import com.google.firebase.FirebaseApp

class MarathonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
