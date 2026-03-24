package com.music.revive

import android.app.Application
import com.music.revive.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReviveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化全局异常捕获
        CrashHandler.init(this)
    }
}
