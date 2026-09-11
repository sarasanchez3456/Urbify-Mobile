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
            // OSM bloquea (403 "tile usage policy") los User-Agent por defecto y los
            // que empiezan por "com.example". Debe ser una cadena descriptiva y única.
            userAgentValue = "UrbifyMobile/1.0 (+https://github.com/sarasanchez3456/Urbify-Mobile)"
            osmdroidBasePath = cacheDir
        }
    }
}
