package com.example.appcrud

import android.app.Application
import com.example.appcrud.data.session.ThemeManager
import com.example.appcrud.data.session.TokenManager
import org.osmdroid.config.Configuration

class UrbifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        ThemeManager.init(this)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
        }
    }
}
