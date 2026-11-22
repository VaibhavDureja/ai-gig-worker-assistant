package com.parkcast.mirror

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val background = Color.rgb(12, 18, 36)
    private val secondary = Color.rgb(164, 184, 212)
    private val white = Color.rgb(239, 246, 255)
    private val mint = Color.rgb(61, 218, 170)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var link: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var copyButton: Button
    private val captureRequest = 1212
    private val refreshUi = object : Runnable {
        override fun run() {
            reflectState()
            handler.postDelayed(this, 600L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(background)
            isFillViewport = true
            if (Build.VERSION.SDK_INT >= 30) {
                setOnApplyWindowInsetsListener { view, insets ->
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    view.setPadding(0, bars.top, 0, bars.bottom)
                    insets
                }
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(26), dp(24), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("PARKCAST  /  OPEN SOURCE", 12, mint, true))
        root.addView(space(14))
        root.addView(label("Your phone.\nOn another screen.", 34, white, true))
        root.addView(space(13))
        root.addView(label(
            "Free screen-casting prototype for devices with a web browser on the same local network.",
            16, secondary, false
        ))
        root.addView(space(28))

        val safetyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(Color.rgb(29, 40, 57), 18)
        }
        safetyCard.addView(label("PARKED USE ONLY", 13, Color.rgb(255, 202, 113), true))
        safetyCard.addView(space(6))
        safetyCard.addView(label(
            "Never watch videos or operate this viewer while driving. This is not an Apple CarPlay or Android Auto app.",
            14, white, false
        ))
        root.addView(safetyCard)
        root.addView(space(24))

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = rounded(Color.rgb(21, 32, 54), 20)
        }
        statusCard.addView(label("CASTING SESSION", 12, secondary, true))
        statusCard.addView(space(8))
        status = label("Not sharing", 22, white, true)
        statusCard.addView(status)
        statusCard.addView(space(12))
        link = label("Start sharing to get a private browser link.", 14, secondary, false)
        link.setTextIsSelectable(true)
        statusCard.addView(link)
        root.addView(statusCard)
        root.addView(space(20))

        startButton = actionButton("Start screen sharing", mint, background) { beginCapture() }
        root.addView(startButton, matchWrap())
        root.addView(space(10))
        stopButton = actionButton("Stop sharing", Color.rgb(64, 80, 108), white) { stopCasting() }
        root.addView(stopButton, matchWrap())
        root.addView(space(10))
        copyButton = actionButton("Copy private viewer link", Color.rgb(31, 48, 73), white) { copyLink() }
        root.addView(copyButton, matchWrap())
        root.addView(space(30))

        root.addView(label("HOW TO TRY IT", 12, mint, true))
        root.addView(space(12))
        for ((number, text) in listOf(
            "1" to "Connect phone and receiving device to the same Wi-Fi (or use a compatible phone hotspot).",
            "2" to "Tap Start, and approve Android's screen-recording prompt.",
            "3" to "Open the private viewer link in the receiving device's browser.",
            "4" to "Tap Stop when finished, or use Stop sharing in the notification."
        )) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(label(number.padStart(2, '0'), 14, mint, true),
                LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT))
            row.addView(label(text, 15, white, false),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            root.addView(row)
        }
        root.addView(space(24))
        root.addView(label(
            "Prototype v0.1 · View-only JPEG stream · No audio · No remote control · No paid features",
            12, secondary, false
        ))
        reflectState()
    }

    private fun beginCapture() {
        if (CastService.active) return
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Foreground capture still works when notifications are denied; permission is optional.
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 323)
        }
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
    }

    @Deprecated("Using the built-in Activity result callback keeps this prototype dependency-free")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequest) return
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Screen sharing cancelled", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, CastService::class.java).apply {
            action = CastService.ACTION_START
            putExtra(CastService.EXTRA_RESULT_CODE, resultCode)
            putExtra(CastService.EXTRA_RESULT_DATA, data)
        }
        startForegroundService(intent)
        status.text = "Starting secure local session…"
    }

    private fun stopCasting() {
        startService(Intent(this, CastService::class.java).setAction(CastService.ACTION_STOP))
    }

    private fun copyLink() {
        val value = CastService.url ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ParkCast private viewer link", value))
        Toast.makeText(this, "Private link copied", Toast.LENGTH_SHORT).show()
    }

    private fun reflectState() {
        val sharing = CastService.active
        status.text = when {
            sharing -> "● Sharing screen"
            CastService.lastError != null -> "Session ended"
            else -> "● Not sharing"
        }
        status.setTextColor(if (sharing) mint else white)
        link.text = when {
            sharing && CastService.url != null -> CastService.url
            sharing -> "Active, but no local network IP found. Connect to Wi-Fi and restart sharing."
            CastService.lastError != null -> CastService.lastError
            else -> "Start sharing to get a private browser link."
        }
        startButton.isEnabled = !sharing
        stopButton.isEnabled = sharing
        copyButton.isEnabled = sharing && CastService.url != null
        startButton.alpha = if (sharing) .45f else 1f
        stopButton.alpha = if (sharing) 1f else .45f
        copyButton.alpha = if (copyButton.isEnabled) 1f else .45f
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshUi)
        handler.post(refreshUi)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshUi)
        super.onPause()
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun actionButton(text: String, fill: Int, foreground: Int, action: () -> Unit) =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            setTextColor(foreground)
            backgroundTintList = ColorStateList.valueOf(fill)
            minHeight = dp(54)
            setOnClickListener { action() }
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun rounded(fill: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()
}
