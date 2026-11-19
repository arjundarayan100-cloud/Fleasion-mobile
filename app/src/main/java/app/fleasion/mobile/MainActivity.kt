package app.fleasion.mobile

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var diagnosticsContainer: LinearLayout

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshStatus() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                refreshStatus()
                statusText.text = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    "Shizuku permission granted. No device changes have been made."
                } else {
                    "Permission not granted. Fleasion remains in standard mode."
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshStatus()
        refreshDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshStatus()
        if (::diagnosticsContainer.isInitialized) refreshDiagnostics()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        super.onDestroy()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(30))
            setBackgroundColor(0xFF080D16.toInt())
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }

        page.addView(text("FLEASION  /  MOBILE", 12f, 0xFF52E3DF.toInt(), true))
        page.addView(text("Your device.\nYour control.", 31f, 0xFFFFFFFF.toInt(), true).apply {
            setPadding(0, dp(10), 0, dp(5))
        })
        page.addView(text("UNIVERSAL ANDROID COMPANION  ·  0.6.0 ALPHA", 10f, 0xFF91A5B9.toInt(), true)
            .apply { setPadding(0, 0, 0, dp(18)) })

        page.addView(cardTitle("DEVICE OVERVIEW"))
        page.addView(infoLine("Manufacturer", Build.MANUFACTURER.orUnknown()))
        page.addView(infoLine("Model", Build.MODEL.orUnknown()))
        page.addView(infoLine("Android version", "Android ${Build.VERSION.RELEASE.orUnknown()}"))
        page.addView(infoLine("API level", Build.VERSION.SDK_INT.toString()))
        page.addView(infoLine("Build", Build.DISPLAY.orUnknown()))
        page.addView(infoLine("Supported ABIs", Build.SUPPORTED_ABIS.joinToString().ifBlank { "Unknown" }))

        page.addView(cardTitle("LIVE DEVICE DIAGNOSTICS"))
        page.addView(text(
            "Read-only information from Android. These values describe device capability and current conditions; they are not Roblox FPS measurements.",
            12f, 0xFF91A5B9.toInt(), false
        ).apply { setPadding(0, 0, 0, dp(8)) })
        diagnosticsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(diagnosticsContainer)
        page.addView(Button(this).apply {
            text = "Refresh diagnostics"
            isAllCaps = false
            setOnClickListener { refreshDiagnostics() }
        })

        page.addView(cardTitle("SHIZUKU CONNECTION"))
        statusText = text("Checking Shizuku…", 14f, 0xFFE8F4FF.toInt(), false)
        statusText.setPadding(0, dp(7), 0, dp(12))
        page.addView(statusText)
        permissionButton = Button(this).apply {
            text = "Request Shizuku permission"
            isAllCaps = false
            setOnClickListener { requestShizukuPermission() }
        }
        page.addView(permissionButton)
        page.addView(Button(this).apply {
            text = "Shizuku setup guide"
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
            }
        })

        page.addView(cardTitle("CAPABILITY-AWARE BY DESIGN"))
        page.addView(text(
            "Fleasion checks Android version, hardware and service availability before exposing future controls. Unsupported features stay unavailable instead of pretending to work. Standard features remain usable without Shizuku.",
            13f, 0xFFD0DCE8.toInt(), false
        ).apply { setPadding(0, dp(5), 0, dp(8)) })
        page.addView(text(
            "Safety: this alpha only reads device information and checks Shizuku status. It does not change system settings, stop other apps, access protected app data, modify Roblox files, inject code or automate gameplay. No FPS increase is promised.",
            11f, 0xFF91A5B9.toInt(), false
        ).apply { setPadding(0, dp(16), 0, 0) })
        setContentView(scroll)
    }

    private fun refreshDiagnostics() {
        if (!::diagnosticsContainer.isInitialized) return
        diagnosticsContainer.removeAllViews()

        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        addDiagnostic("Available memory", formatBytes(memory.availMem))
        addDiagnostic("Total memory", formatBytes(memory.totalMem))
        addDiagnostic("Memory pressure", if (memory.lowMemory) "Low-memory condition reported" else "No low-memory warning")

        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        addDiagnostic("Current display rate", String.format(Locale.US, "%.1f Hz", display.refreshRate))
        val modes = runCatching { display.supportedModes.map { it.refreshRate }.distinct().sorted() }
            .getOrDefault(emptyList())
        addDiagnostic("Reported display modes", if (modes.isEmpty()) "Not reported" else
            modes.joinToString { String.format(Locale.US, "%.0f Hz", it) })

        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra("level", -1) ?: -1
        val scale = batteryIntent?.getIntExtra("scale", -1) ?: -1
        addDiagnostic("Battery level", if (level >= 0 && scale > 0) "${(level * 100) / scale}%" else "Unavailable")

        val storage = runCatching { StatFs(Environment.getDataDirectory().absolutePath) }.getOrNull()
        addDiagnostic("Available internal storage", storage?.let { formatBytes(it.availableBytes) } ?: "Unavailable")

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val power = getSystemService(POWER_SERVICE) as PowerManager
            runCatching { thermalLabel(power.currentThermalStatus) }.getOrDefault("Unavailable")
        } else "Not exposed on this Android version"
        addDiagnostic("Thermal status", thermal)
    }

    private fun thermalLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "No thermal throttling reported"
        PowerManager.THERMAL_STATUS_LIGHT -> "Light"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown threshold"
        else -> "Unknown"
    }

    private fun addDiagnostic(label: String, value: String) {
        diagnosticsContainer.addView(infoLine(label, value))
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Unavailable"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var amount = bytes.toDouble()
        var unit = 0
        while (amount >= 1024 && unit < units.lastIndex) { amount /= 1024; unit++ }
        return String.format(Locale.US, "%.1f %s", amount, units[unit])
    }

    private fun refreshStatus() {
        val connected = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = connected && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        statusText.text = when {
            !connected -> "Not connected. Start Shizuku, then return here."
            granted -> "Connected · permission granted"
            else -> "Connected · permission not granted"
        }
        permissionButton.isEnabled = connected && !granted
        permissionButton.text = if (granted) "Permission granted" else "Request Shizuku permission"
    }

    private fun requestShizukuPermission() {
        if (runCatching { Shizuku.isPreV11() }.getOrDefault(true)) {
            statusText.text = "This Shizuku service version is not supported. Update Shizuku and retry."
            return
        }
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            statusText.text = "Shizuku is not running. Start it first, then retry."
            refreshStatus()
            return
        }
        if (runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)) {
            refreshStatus()
            return
        }
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { statusText.text = "Could not request permission: ${it.message ?: "unknown error"}" }
    }

    private fun cardTitle(value: String) = text(value, 11f, 0xFF52E3DF.toInt(), true)
        .apply { setPadding(0, dp(22), 0, dp(8)) }

    private fun infoLine(label: String, value: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }
        row.addView(text(label, 12f, 0xFF91A5B9.toInt(), false), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(value, 12f, 0xFFFFFFFF.toInt(), true), LinearLayout.LayoutParams(0, -2, 1.35f))
        return row
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun String?.orUnknown(): String = this?.takeIf { it.isNotBlank() } ?: "Unknown"

    companion object { private const val REQUEST_CODE = 7301 }
}
