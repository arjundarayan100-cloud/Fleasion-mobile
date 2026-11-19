package app.fleasion.mobile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.app.ActivityManager
import android.view.WindowManager
import org.json.JSONObject
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingExport: String? = null
    private var pendingExportName: String = "fleasion-export.json"

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshPageStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshPageStatus() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                refreshPageStatus()
                Toast.makeText(
                    this,
                    if (grantResult == PackageManager.PERMISSION_GRANTED)
                        "Shizuku permission granted"
                    else "Shizuku permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.setBackgroundColor(0xFF080D16.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.addJavascriptInterface(NativeBridge(), "FleasionNative")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "file" || uri.scheme == "about") return false
                if (uri.scheme == "https" || uri.scheme == "http") {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        .onFailure { Toast.makeText(this@MainActivity, "No browser available", Toast.LENGTH_SHORT).show() }
                    return true
                }
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = callback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
                }
                return try {
                    startActivityForResult(intent, IMPORT_REQUEST_CODE)
                    true
                } catch (_: Exception) {
                    fileChooserCallback = null
                    callback.onReceiveValue(null)
                    false
                }
            }
        }
        setContentView(webView)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // Restore the existing workspace view.
        } else {
            webView.loadUrl("file:///android_asset/index.html")
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) refreshPageStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMPORT_REQUEST_CODE -> {
                val result = if (resultCode == RESULT_OK && data?.data != null)
                    arrayOf(data.data!!) else null
                fileChooserCallback?.onReceiveValue(result)
                fileChooserCallback = null
            }
            EXPORT_REQUEST_CODE -> {
                val uri = if (resultCode == RESULT_OK) data?.data else null
                val content = pendingExport
                if (uri != null && content != null) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(content.toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open the selected document")
                    }.onSuccess {
                        Toast.makeText(this, "JSON file saved", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(this, "Export failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
                pendingExport = null
            }
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        fileChooserCallback?.onReceiveValue(null)
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("FleasionNative")
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun shizukuState(): String {
        val connected = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!connected) return "Shizuku service not detected"
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return if (granted) "Permission granted" else "Connected · permission needed"
    }

    private fun deviceInfoJson(): String {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val storage = runCatching { StatFs(Environment.getDataDirectory().absolutePath).availableBytes }.getOrDefault(-1L)
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val power = getSystemService(POWER_SERVICE) as PowerManager
            runCatching { thermalLabel(power.currentThermalStatus) }.getOrDefault("Unavailable")
        } else "Not exposed"
        return JSONObject()
            .put("platform", "Android")
            .put("model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            .put("android", "Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
            .put("cores", Runtime.getRuntime().availableProcessors().toString())
            .put("memoryHint", "About ${memory.totalMem / (1024L * 1024L * 1024L)} GB")
            .put("totalRam", formatBytes(memory.totalMem))
            .put("availableRam", formatBytes(memory.availMem))
            .put("screen", "${resources.displayMetrics.widthPixels} × ${resources.displayMetrics.heightPixels} · ${resources.displayMetrics.densityDpi} dpi")
            .put("refreshRate", String.format(java.util.Locale.US, "%.1f Hz", display.refreshRate))
            .put("freeStorage", formatBytes(storage))
            .put("thermal", thermal)
            .toString()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Unavailable"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var amount = bytes.toDouble()
        var unit = 0
        while (amount >= 1024 && unit < units.lastIndex) { amount /= 1024; unit++ }
        return String.format(java.util.Locale.US, "%.1f %s", amount, units[unit])
    }

    private fun thermalLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Light"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown threshold"
        else -> "Unknown"
    }

    private fun refreshPageStatus() {
        if (!::webView.isInitialized) return
        webView.post {
            webView.evaluateJavascript(
                "if (window.refreshNativeShizuku) window.refreshNativeShizuku(); if (window.refreshNativeDeviceSnapshot) window.refreshNativeDeviceSnapshot();",
                null
            )
        }
    }

    private fun requestShizukuPermission() {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            AlertDialog.Builder(this)
                .setTitle("Shizuku is not running")
                .setMessage("Start Shizuku using its official setup guide, then return to Fleasion.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        if (runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)) {
            refreshPageStatus()
            Toast.makeText(this, "Permission is already granted", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Allow Fleasion to use Shizuku?")
            .setMessage(
                "This permission is optional. The current build only checks the connection and reads device diagnostics. " +
                "It does not change Android settings, stop apps, modify Roblox files or automate gameplay."
            )
            .setNegativeButton("Not now", null)
            .setPositiveButton("Continue") { _, _ ->
                runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST_CODE) }
                    .onFailure {
                        Toast.makeText(this, "Permission request failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .show()
    }

    private fun launchExport(filename: String, content: String) {
        pendingExportName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_").take(90)
            .ifBlank { "fleasion-export.json" }
        pendingExport = content
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, pendingExportName)
        }
        try {
            startActivityForResult(intent, EXPORT_REQUEST_CODE)
        } catch (_: Exception) {
            pendingExport = null
            Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show()
        }
    }

    inner class NativeBridge {
        @JavascriptInterface
        fun getShizukuState(): String = shizukuState()

        @JavascriptInterface
        fun getDeviceInfo(): String = deviceInfoJson()

        @JavascriptInterface
        fun requestShizukuPermission() {
            runOnUiThread { this@MainActivity.requestShizukuPermission() }
        }

        @JavascriptInterface
        fun saveJsonFile(filename: String, content: String) {
            if (content.length > MAX_EXPORT_CHARS) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Export is too large", Toast.LENGTH_SHORT).show() }
                return
            }
            runOnUiThread { launchExport(filename, content) }
        }
    }

    companion object {
        private const val SHIZUKU_REQUEST_CODE = 7301
        private const val IMPORT_REQUEST_CODE = 7302
        private const val EXPORT_REQUEST_CODE = 7303
        private const val MAX_EXPORT_CHARS = 2_000_000
    }
}
