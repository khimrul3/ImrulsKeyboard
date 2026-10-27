package com.imrulskeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val bg = Color.rgb(5, 5, 6)
    private val blue = Color.rgb(18, 88, 174)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "IMRULS\nKEYBOARD"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.08f
            includeFontPadding = false
        }
        content.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "English keyboard • Colorful dotted-key theme"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(18), 0, dp(30))
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, -2))
        content.addView(makeButton("Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, fullWidth())
        content.addView(makeButton("Choose keyboard") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }, fullWidth().apply { topMargin = dp(12) })
        content.addView(makeButton("Choose custom keyboard background") {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
            startActivityForResult(pick, 712)
        }, fullWidth().apply { topMargin = dp(12) })
        content.addView(makeButton("Enable voice typing permission") {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 713)
            else makeToast("Microphone permission already granted")
        }, fullWidth().apply { topMargin = dp(12) })

        val note = TextView(this).apply {
            text = "1. Enable Imruls Keyboard in system settings.\n2. Choose it from the keyboard picker.\n3. Open the keyboard settings icon to choose 22 RGB themes.\n\nEnglish • Mechanical keys • Suggestions • Voice input • Custom background"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(dp(12), dp(24), dp(12), 0)
        }
        content.addView(note, fullWidth())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
            addView(content, ViewGroup.LayoutParams(-1, -2))
        }
        setContentView(scroll)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 712 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE).edit().putString("background_uri", uri.toString()).apply()
            makeToast("Background saved. Reopen the keyboard to apply it.")
        }
    }

    private fun makeToast(message: String) = android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    private fun fullWidth() = LinearLayout.LayoutParams(-1, dp(56))

    private fun makeButton(label: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            includeFontPadding = false
            background = GradientDrawable().apply {
                setColor(blue)
                cornerRadius = dp(14).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
