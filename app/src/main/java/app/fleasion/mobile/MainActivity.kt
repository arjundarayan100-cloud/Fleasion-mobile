package app.fleasion.mobile

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshStatus() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                refreshStatus()
                statusText.text = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    "Shizuku permission granted. No changes have been made to your device."
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
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(0xFF0B1020.toInt())
        }
        val scroll = ScrollView(this).apply { addView(page) }

        page.addView(text("FLEASION", 13f, 0xFF8DA8FF.toInt(), true))
        page.addView(text("Mobile 0.6.0", 30f, 0xFFFFFFFF.toInt(), true))
        page.addView(text("UNIVERSAL ANDROID COMPANION", 11f, 0xFF9BA8C5.toInt(), true).apply {
            setPadding(0, dp(3), 0, dp(22))
        })

        page.addView(cardTitle("YOUR DEVICE"))
        page.addView(infoLine("Manufacturer", Build.MANUFACTURER.orUnknown()))
        page.addView(infoLine("Model", Build.MODEL.orUnknown()))
        page.addView(infoLine("Android", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"))
        page.addView(infoLine("Build", Build.DISPLAY.orUnknown()))

        page.addView(cardTitle("SHIZUKU CONNECTION"))
        statusText = text("Checking Shizuku…", 15f, 0xFFE8EDFF.toInt(), false)
        statusText.setPadding(0, dp(8), 0, dp(12))
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

        page.addView(cardTitle("WHAT THIS BUILD DOES"))
        page.addView(text(
            "• Detects basic device and Android information\n" +
            "• Detects whether Shizuku is connected\n" +
            "• Requests permission only after you tap the button\n" +
            "• Keeps standard features available without Shizuku",
            14f, 0xFFD0D8EF.toInt(), false
        ).apply { setPadding(0, dp(6), 0, dp(12)) })

        page.addView(text(
            "Safety note: this preview does not change system settings, terminate apps, modify Roblox files, or promise higher game FPS. Shizuku permission alone does not guarantee that a system operation is supported.",
            12f, 0xFF9BA8C5.toInt(), false
        ).apply { setPadding(0, dp(14), 0, 0) })

        setContentView(scroll)
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
        if (runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        ) {
            refreshStatus()
            return
        }
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { statusText.text = "Could not request permission: ${it.message ?: "unknown error"}" }
    }

    private fun cardTitle(value: String) = text(value, 12f, 0xFF8DA8FF.toInt(), true).apply {
        setPadding(0, dp(24), 0, dp(8))
    }

    private fun infoLine(label: String, value: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }
        row.addView(text(label, 13f, 0xFF9BA8C5.toInt(), false), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(value, 13f, 0xFFFFFFFF.toInt(), true), LinearLayout.LayoutParams(0, -2, 1.2f))
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
