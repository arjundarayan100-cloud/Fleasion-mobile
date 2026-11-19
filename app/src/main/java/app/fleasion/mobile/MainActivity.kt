package app.fleasion.mobile

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var details: TextView

    private val permissionRequestCode = 4102

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { refreshShizukuStatus() }
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread { refreshShizukuStatus() }
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == permissionRequestCode) {
                runOnUiThread {
                    status.text = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        "Shizuku permission granted"
                    } else {
                        "Shizuku permission not granted"
                    }
                    refreshShizukuStatus()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 15, 27)
        window.navigationBarColor = Color.rgb(12, 15, 27)
        window.decorView.systemUiVisibility = 0
        buildScreen()
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshShizukuStatus()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(12, 15, 27))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val brand = TextView(this).apply {
            text = "FLEASION"
            textSize = 13f
            letterSpacing = 0.22f
            setTextColor(Color.rgb(133, 153, 255))
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(brand)

        val heading = TextView(this).apply {
            text = "Android Edition"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 4)
        }
        root.addView(heading)

        val subtitle = TextView(this).apply {
            text = "Native companion • v0.1.0"
            textSize = 14f
            setTextColor(Color.LTGRAY)
        }
        root.addView(subtitle)

        val shizukuTitle = sectionTitle("SHIZUKU CONNECTION")
        root.addView(shizukuTitle)
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 12)
        }
        root.addView(status)

        val connectButton = Button(this).apply {
            text = "Check / request Shizuku access"
            setOnClickListener { connectShizuku() }
        }
        root.addView(connectButton)

        root.addView(sectionTitle("DEVICE INFORMATION"))
        details = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 12)
            text = "Loading device information…"
        }
        root.addView(details)
        details.text = "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}"

        root.addView(sectionTitle("COMING NEXT"))
        val next = TextView(this).apply {
            text = "• Import and export Fleasion profiles\n• Manage Fleasion's own files\n• Backup and restore app settings\n• Permission and operation history"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 12)
        }
        root.addView(next)

        val note = TextView(this).apply {
            text = "Safety note: Fleasion will only manage files and settings you are authorized to access. It will not bypass Roblox or Android app protections."
            textSize = 12f
            setTextColor(Color.rgb(173, 181, 199))
            setPadding(0, 18, 0, 0)
        }
        root.addView(note)
        root.gravity = Gravity.TOP
        setContentView(root)
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        letterSpacing = 0.12f
        setTextColor(Color.rgb(133, 153, 255))
        setPadding(0, 28, 0, 0)
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun connectShizuku() {
        if (!Shizuku.pingBinder()) {
            status.text = "Shizuku is not running. Start the Shizuku service, then try again."
            return
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            status.text = "Connected • permission granted"
        } else {
            status.text = "Shizuku is running • requesting permission…"
            Shizuku.requestPermission(permissionRequestCode)
        }
    }

    private fun refreshShizukuStatus() {
        status.text = when {
            !Shizuku.pingBinder() -> "Not connected • start Shizuku to continue"
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED ->
                "Connected • permission granted"
            else -> "Shizuku detected • permission not granted"
        }
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        super.onDestroy()
    }
}
