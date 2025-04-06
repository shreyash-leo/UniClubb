package com.example.uniclubb

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryConfig {
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "ddzf4jpbm",
                "api_key" to "581451194512555",  // ⚠️ Avoid hardcoding
                "api_secret" to "qt8tQMpSyQuz51ZTMOFwCrcSlPo"  // ⚠️ Move this to a secure location
            )
            MediaManager.init(context, config)
            isInitialized = true
        }
    }
}
