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
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var diagnosticsContainer: LinearLayout
    private lateinit var workspaceWebView: WebView
    private lateinit var workspaceButton: Button
    private lateinit var deviceButton: Button
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingJsonExport: String? = null

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
        if (::workspaceWebView.isInitialized) workspaceWebView.destroy()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == SAVE_JSON_REQUEST) {
            val uri = resultData?.data
            val content = pendingJsonExport
            pendingJsonExport = null
            if (resultCode == RESULT_OK && uri != null && content != null) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                        ?: error("Could not open the selected file")
                }.onSuccess {
                    Toast.makeText(this, "Fleasion backup saved", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(this, "Export failed: ${it.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == PICK_JSON_REQUEST) {
            val callback = fileChooserCallback
            fileChooserCallback = null
            callback?.onReceiveValue(
                if (resultCode == RESULT_OK && resultData?.data != null) arrayOf(resultData.data!!) else null
            )
        }
    }

    private fun buildScreen() {
        val diagnosticsPage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(30))
            setBackgroundColor(0xFF080D16.toInt())
        }
        val diagnosticsScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(diagnosticsPage)
        }

        diagnosticsPage.addView(text("FLEASION  /  MOBILE", 12f, 0xFF52E3DF.toInt(), true))
        diagnosticsPage.addView(text("Your device.\nYour control.", 31f, 0xFFFFFFFF.toInt(), true).apply {
            setPadding(0, dp(10), 0, dp(5))
        })
        diagnosticsPage.addView(text("UNIVERSAL ANDROID COMPANION  ·  0.7.0 ALPHA", 10f, 0xFF91A5B9.toInt(), true)
            .apply { setPadding(0, 0, 0, dp(18)) })

        diagnosticsPage.addView(cardTitle("DEVICE OVERVIEW"))
        diagnosticsPage.addView(infoLine("Manufacturer", Build.MANUFACTURER.orUnknown()))
        diagnosticsPage.addView(infoLine("Model", Build.MODEL.orUnknown()))
        diagnosticsPage.addView(infoLine("Android version", "Android ${Build.VERSION.RELEASE.orUnknown()}"))
        diagnosticsPage.addView(infoLine("API level", Build.VERSION.SDK_INT.toString()))
        diagnosticsPage.addView(infoLine("Build", Build.DISPLAY.orUnknown()))
        diagnosticsPage.addView(infoLine("Supported ABIs", Build.SUPPORTED_ABIS.joinToString().ifBlank { "Unknown" }))

        diagnosticsPage.addView(cardTitle("LIVE DEVICE DIAGNOSTICS"))
        diagnosticsPage.addView(text(
            "Read-only information from Android. These values describe device capability and current conditions; they are not Roblox FPS measurements.",
            12f, 0xFF91A5B9.toInt(), false
        ).apply { setPadding(0, 0, 0, dp(8)) })
        diagnosticsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        diagnosticsPage.addView(diagnosticsContainer)
        diagnosticsPage.addView(Button(this).apply {
            text = "Refresh diagnostics"
            isAllCaps = false
            setOnClickListener { refreshDiagnostics() }
        })

        diagnosticsPage.addView(cardTitle("FPS READINESS · SAFE OPTIMIZATION"))
        diagnosticsPage.addView(text(
            "Fleasion checks memory pressure and thermal status, then lets you open Android's own settings. These are device-level steps, not a guaranteed in-game FPS boost.",
            12f, 0xFF91A5B9.toInt(), false
        ).apply { setPadding(0, 0, 0, dp(10)) })
        diagnosticsPage.addView(Button(this).apply {
            text = "Open Roblox app settings"
            isAllCaps = false
            setOnClickListener {
                runCatching {
                    startActivity(Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:com.roblox.client")
                    ))
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Could not open app settings", Toast.LENGTH_SHORT).show()
                }
            }
        })
        diagnosticsPage.addView(Button(this).apply {
            text = "Open display settings"
            isAllCaps = false
            setOnClickListener {
                runCatching { startActivity(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)) }
                    .onFailure { Toast.makeText(this@MainActivity, "Display settings unavailable", Toast.LENGTH_SHORT).show() }
            }
        })
        diagnosticsPage.addView(text(
            "For a 4 GB RAM phone: keep some storage free, close apps you are not using, avoid playing while the phone is hot, and use only refresh-rate options your display actually supports. Do not use RAM-cleaner apps or force-stop system services.",
            11f, 0xFFD0DCE8.toInt(), false
        ).apply { setPadding(0, dp(8), 0, dp(8)) })

        diagnosticsPage.addView(cardTitle("SHIZUKU CONNECTION"))
        statusText = text("Checking Shizuku…", 14f, 0xFFE8F4FF.toInt(), false)
        statusText.setPadding(0, dp(7), 0, dp(12))
        diagnosticsPage.addView(statusText)
        permissionButton = Button(this).apply {
            text = "Request Shizuku permission"
            isAllCaps = false
            setOnClickListener { requestShizukuPermission() }
        }
        diagnosticsPage.addView(permissionButton)
        diagnosticsPage.addView(Button(this).apply {
            text = "Shizuku setup guide"
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
            }
        })

        diagnosticsPage.addView(cardTitle("CAPABILITY-AWARE BY DESIGN"))
        diagnosticsPage.addView(text(
            "Fleasion checks Android version, hardware and service availability before exposing controls. Unsupported features stay unavailable instead of pretending to work. The full profile and rule workspace is available in the Workspace tab.",
            13f, 0xFFD0DCE8.toInt(), false
        ).apply { setPadding(0, dp(5), 0, dp(8)) })
        diagnosticsPage.addView(text(
            "Safety: this build reads device information and checks Shizuku status. It does not change system settings, access protected app data, modify Roblox files, inject code or automate gameplay.",
            11f, 0xFF91A5B9.toInt(), false
        ).apply { setPadding(0, dp(16), 0, 0) })

        workspaceWebView = WebView(this).apply {
            setBackgroundColor(0xFF080D16.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportZoom(false)
            isVerticalScrollBarEnabled = false
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
                    }
                    return try {
                        startActivityForResult(intent, PICK_JSON_REQUEST)
                        true
                    } catch (_: Exception) {
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = null
                        false
                    }
                }
            }
            addJavascriptInterface(WorkspaceBridge(), "FleasionNative")
            loadUrl("file:///android_asset/workspace/index.html")
        }

        val content = FrameLayout(this)
        content.addView(workspaceWebView, FrameLayout.LayoutParams(-1, -1))
        content.addView(diagnosticsScroll, FrameLayout.LayoutParams(-1, -1))
        diagnosticsScroll.visibility = View.GONE

        // Persistent bottom navigation: content stays above these tabs.
        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            setBackgroundColor(0xFF0C1522.toInt())
        }
        workspaceButton = navButton("▣  Workspace", true) { showWorkspace() }
        deviceButton = navButton("⚙  Device + Shizuku", false) { showDevice() }
        navigation.addView(workspaceButton, LinearLayout.LayoutParams(0, dp(50), 1f).apply {
            marginEnd = dp(5)
        })
        navigation.addView(deviceButton, LinearLayout.LayoutParams(0, dp(50), 1f).apply {
            marginStart = dp(5)
        })

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF080D16.toInt())
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(navigation, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(root)
        showWorkspace()
    }

    private fun navButton(label: String, selected: Boolean, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setPadding(dp(16), 0, dp(12), 0)
            setTextColor(if (selected) 0xFF06151B.toInt() else 0xFFE8F4FF.toInt())
            setBackgroundColor(if (selected) 0xFF52E3DF.toInt() else 0xFF18283A.toInt())
            setOnClickListener { action() }
        }

    private fun showWorkspace() {
        workspaceWebView.visibility = View.VISIBLE
        // The diagnostics view is the second child of the content frame.
        (workspaceWebView.parent as? FrameLayout)?.getChildAt(1)?.visibility = View.GONE
        workspaceButton.setBackgroundColor(0xFF52E3DF.toInt())
        workspaceButton.setTextColor(0xFF06151B.toInt())
        deviceButton.setBackgroundColor(0xFF18283A.toInt())
        deviceButton.setTextColor(0xFFE8F4FF.toInt())
    }

    private fun showDevice() {
        workspaceWebView.visibility = View.GONE
        (workspaceWebView.parent as? FrameLayout)?.getChildAt(1)?.visibility = View.VISIBLE
        deviceButton.setBackgroundColor(0xFF52E3DF.toInt())
        deviceButton.setTextColor(0xFF06151B.toInt())
        workspaceButton.setBackgroundColor(0xFF18283A.toInt())
        workspaceButton.setTextColor(0xFFE8F4FF.toInt())
        refreshStatus()
        refreshDiagnostics()
    }

    private inner class WorkspaceBridge {
        @JavascriptInterface
        fun getDeviceSnapshot(): String {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val memory = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memory)
            val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra("level", -1) ?: -1
            val scale = batteryIntent?.getIntExtra("scale", -1) ?: -1
            val storage = runCatching { StatFs(Environment.getDataDirectory().absolutePath) }.getOrNull()
            val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val power = getSystemService(POWER_SERVICE) as PowerManager
                runCatching { power.currentThermalStatus }.getOrDefault(-1)
            } else -1
            val thermal = if (thermalStatus >= 0) thermalLabel(thermalStatus) else "Not exposed"
            val json = org.json.JSONObject()
            json.put("availableRam", formatBytes(memory.availMem))
            json.put("totalRam", formatBytes(memory.totalMem))
            json.put("memoryPressure", memory.lowMemory)
            json.put("refreshRate", String.format(Locale.US, "%.1f Hz", display.refreshRate))
            json.put("thermal", thermal)
            json.put("thermalWarning", thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE)
            json.put("freeStorage", storage?.let { formatBytes(it.availableBytes) } ?: "Unavailable")
            json.put("battery", if (level >= 0 && scale > 0) "${(level * 100) / scale}%" else "Unavailable")
            return json.toString()
        }

        @JavascriptInterface
        fun openRobloxSettings() {
            runOnUiThread {
                runCatching {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.roblox.client")))
                }.onFailure { Toast.makeText(this@MainActivity, "Roblox app settings unavailable", Toast.LENGTH_SHORT).show() }
            }
        }

        @JavascriptInterface
        fun openDisplaySettings() {
            runOnUiThread {
                runCatching { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
                    .onFailure { Toast.makeText(this@MainActivity, "Display settings unavailable", Toast.LENGTH_SHORT).show() }
            }
        }

        @JavascriptInterface
        fun saveJson(content: String, filename: String) {
            runOnUiThread {
                pendingJsonExport = content
                val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "-").take(80)
                    .ifBlank { "fleasion-backup.json" }
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, safeName)
                }
                try {
                    startActivityForResult(intent, SAVE_JSON_REQUEST)
                } catch (_: Exception) {
                    pendingJsonExport = null
                    Toast.makeText(this@MainActivity, "No file picker is available", Toast.LENGTH_LONG).show()
                }
            }
        }
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

    companion object {
        private const val REQUEST_CODE = 7301
        private const val SAVE_JSON_REQUEST = 7302
        private const val PICK_JSON_REQUEST = 7303
    }
}
