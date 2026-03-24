package com.music.revive.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {
    
    private const val EXTRA_CRASH_INFO = "crash_info"
    
    private lateinit var context: Context
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    
    fun init(context: Context) {
        this.context = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val crashInfo = collectCrashInfo(throwable)
        saveCrashLog(crashInfo)
        startCrashReportActivity(crashInfo)
        
        // 调用默认处理器（如果有的话）
        defaultHandler?.uncaughtException(thread, throwable) ?: run {
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }
    
    private fun collectCrashInfo(throwable: Throwable): String {
        val sb = StringBuilder()
        
        // 时间戳
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date())
        sb.appendLine("=== Revive Music Player Crash Report ===")
        sb.appendLine("Time: $timestamp")
        sb.appendLine()
        
        // 设备信息
        sb.appendLine("--- Device Info ---")
        sb.appendLine("Brand: ${Build.BRAND}")
        sb.appendLine("Device: ${Build.DEVICE}")
        sb.appendLine("Model: ${Build.MODEL}")
        sb.appendLine("Product: ${Build.PRODUCT}")
        sb.appendLine("Manufacturer: ${Build.MANUFACTURER}")
        sb.appendLine()
        
        // 系统信息
        sb.appendLine("--- System Info ---")
        sb.appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Android Codename: ${Build.VERSION.CODENAME}")
        sb.appendLine()
        
        // 应用信息
        sb.appendLine("--- App Info ---")
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("Version Name: ${packageInfo.versionName}")
            sb.appendLine("Version Code: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode}")
        } catch (e: Exception) {
            sb.appendLine("Version: Unable to get version info")
        }
        sb.appendLine("Package: ${context.packageName}")
        sb.appendLine()
        
        // 堆栈跟踪
        sb.appendLine("--- Stack Trace ---")
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        sb.appendLine(stringWriter.toString())
        sb.appendLine()
        
        // 原因链
        var cause: Throwable? = throwable.cause
        while (cause != null) {
            sb.appendLine("--- Caused by: ${cause.javaClass.name} ---")
            sb.appendLine("Message: ${cause.message}")
            val causeWriter = StringWriter()
            cause.printStackTrace(PrintWriter(causeWriter))
            sb.appendLine(causeWriter.toString())
            cause = cause.cause
        }
        
        sb.appendLine("=== End of Crash Report ===")
        
        return sb.toString()
    }
    
    private fun saveCrashLog(crashInfo: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "crash_$timestamp.log"
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(crashInfo.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            // 忽略保存错误
        }
    }
    
    private fun startCrashReportActivity(crashInfo: String) {
        try {
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_CRASH_INFO, crashInfo)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果无法启动 Activity，记录错误
            e.printStackTrace()
        }
    }
}
