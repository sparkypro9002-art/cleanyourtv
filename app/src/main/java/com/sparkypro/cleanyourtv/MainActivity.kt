package com.sparkypro.cleanyourtv

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private lateinit var statsText: TextView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(80, 50, 80, 50)
        }

        val title = TextView(this).apply {
            text = "Clean Your TV  •  Mi TV Stick (Android 9)"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }

        statsText = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#E2E8F0"))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 28)
        }

        statusText = TextView(this).apply {
            text = "Use your remote D-Pad to select an action below"
            textSize = 16f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val btnBoostRam = createTvButton("🚀  Free Up RAM & Stop Background Apps") {
            boostRam()
        }

        val btnClearCache = createTvButton("🧹  Clear App & Temp Cache") {
            clearLocalCache()
        }

        val btnStorageSettings = createTvButton("💾  Open Android TV Storage & Cached Data") {
            openStorageSettings()
        }

        val btnManageApps = createTvButton("⚙️  Open Installed Apps List (Force Stop / Uninstall)") {
            openAppsSettings()
        }

        root.addView(title)
        root.addView(statsText)
        root.addView(statusText)
        root.addView(btnBoostRam)
        root.addView(btnClearCache)
        root.addView(btnStorageSettings)
        root.addView(btnManageApps)

        setContentView(root)
        updateStats()
        btnBoostRam.requestFocus()
    }

    private fun createTvButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 18f
            isAllCaps = false
            setTextColor(Color.WHITE)
            isFocusable = true
            isFocusableInTouchMode = true

            val normalBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#334155"))
            }
            val focusedBg = GradientDrawable().apply {
                setColor(Color.parseColor("#0284C7"))
                cornerRadius = 16f
                setStroke(4, Color.parseColor("#7DD3FC"))
            }

            background = normalBg
            setOnFocusChangeListener { _, hasFocus ->
                background = if (hasFocus) focusedBg else normalBg
            }

            val params = LinearLayout.LayoutParams(900, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 12, 0, 12)
            }
            layoutParams = params
            setPadding(32, 22, 32, 22)
            setOnClickListener { onClick() }
        }
    }

    private fun boostRam() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val beforeMem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var killedCount = 0
        for (app in packages) {
            if (app.packageName != packageName) {
                am.killBackgroundProcesses(app.packageName)
                killedCount++
            }
        }

        System.gc()
        val afterMem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val freedMb = ((afterMem.availMem - beforeMem.availMem) / (1024 * 1024)).coerceAtLeast(0)

        statusText.setTextColor(Color.parseColor("#4ADE80"))
        statusText.text = "✅ Sent stop signal to $killedCount background packages (Freed ~${freedMb} MB RAM)"
        updateStats()
    }

    private fun clearLocalCache() {
        var deletedBytes = 0L
        listOfNotNull(cacheDir, externalCacheDir).forEach { dir ->
            deletedBytes += dir.walkBottomUp().fold(0L) { acc, file ->
                val len = if (file.isFile) file.length() else 0L
                if (file != dir && file.delete()) acc + len else acc
            }
        }
        statusText.setTextColor(Color.parseColor("#4ADE80"))
        statusText.text = "✅ Cleared internal temp cache (${deletedBytes / 1024} KB). Use Storage button below for system-wide cache."
        updateStats()
    }

    private fun openStorageSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openAppsSettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun updateStats() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val availStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        val totalStorageMb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)

        statsText.text = "RAM Free: ${availRamMb} MB / ${totalRamMb} MB   |   Storage Free: ${availStorageMb} MB / ${totalStorageMb} MB"
    }
}
