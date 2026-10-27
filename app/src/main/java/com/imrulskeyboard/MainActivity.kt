package com.imrulskeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
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

        val note = TextView(this).apply {
            text = "1. Enable Imruls Keyboard in system settings.\n2. Choose Imruls Keyboard from the keyboard picker.\n\nEnglish only • Black premium theme"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(dp(12), dp(24), dp(12), 0)
        }
        content.addView(note, fullWidth())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
            addView(content, ScrollView.LayoutParams(-1, -2))
        }
        setContentView(scroll)
    }

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
