package com.example.appcrud

import android.app.Application
import com.example.appcrud.data.session.ThemeManager
import com.example.appcrud.data.session.TokenManager

class UrbifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        ThemeManager.init(this)
    }
}
