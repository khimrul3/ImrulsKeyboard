package com.imrulskeyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

class KeyboardService : InputMethodService() {
    private enum class Mode { ENGLISH, PHONETIC, BANGLA }
    private var mode = Mode.ENGLISH
    private var symbols = false
    private var phoneticBuffer = ""
    private var root: LinearLayout? = null

    private val black = Color.rgb(0, 0, 0)
    private val dark = Color.rgb(25, 28, 32)
    private val cream = Color.rgb(244, 237, 233)
    private val yellow = Color.rgb(239, 190, 43)
    private val blue = Color.rgb(43, 91, 151)
    private val red = Color.rgb(198, 48, 49)

    override fun onCreateInputView(): View {
        return buildKeyboard()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        phoneticBuffer = ""
        root = null
    }

    private fun buildKeyboard(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(black)
            setPadding(dp(4), dp(8), dp(4), dp(10))
        }
        root = container

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        top.addView(key("⌄", dark, Color.WHITE) { requestHideSelf(0) }, weighted(0.7f, 42))
        top.addView(key(modeLabel(), dark, Color.WHITE) { cycleMode() }, weighted(1.6f, 42))
        top.addView(key("◎", dark, Color.WHITE) { cycleMode() }, weighted(0.7f, 42))
        container.addView(top)

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(8))
        }
        val suggestionsText = when (mode) {
            Mode.ENGLISH -> listOf("Hello", "How", "Thanks")
            Mode.PHONETIC -> if (phoneticBuffer.isBlank()) listOf("বাংলা", "আমি", "হ্যালো") else listOf(BanglaPhonetic.convert(phoneticBuffer), "আমি", "ভালো")
            Mode.BANGLA -> listOf("আমি", "আপনি", "বাংলা")
        }
        suggestionsText.forEach { word ->
            suggestions.addView(key(word, dark, Color.WHITE) {
                val ic = currentInputConnection ?: return@key
                if (mode == Mode.PHONETIC && phoneticBuffer.isNotEmpty()) {
                    ic.commitText(BanglaPhonetic.convert(phoneticBuffer), 1)
                    phoneticBuffer = ""
                } else {
                    ic.commitText(word, 1)
                }
                refresh()
            }, weighted(1f, 42))
        }
        container.addView(suggestions)

        if (symbols) {
            listOf("1234567890", "@#\$%&-*+/", "()[]{}!?;:", "ABC", "space", "⌫").forEach { row ->
                val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                row.split("").filter { it.isNotEmpty() }.forEach { char ->
                    line.addView(key(char, colorFor(char), textFor(colorFor(char))) { press(char) }, weighted(1f, 48))
                }
                if (row == "ABC") {
                    line.removeAllViews()
                    line.addView(key("ABC", red, Color.WHITE) { symbols = false; refresh() }, weighted(1f, 48))
                    line.addView(key("space", dark, Color.WHITE) { press(" ") }, weighted(3f, 48))
                    line.addView(key("⌫", cream, Color.BLACK) { press("⌫") }, weighted(1f, 48))
                }
                container.addView(line)
            }
        } else {
            val rows = when (mode) {
                Mode.BANGLA -> listOf(
                    "ৌৈাীূবহগদ",
                    "োে্িুপটচজ",
                    "ংমনতরলসয",
                )
                else -> listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
            }
            rows.forEachIndexed { idx, row ->
                val line = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                if (idx == 2) {
                    line.addView(key("⇧", yellow, Color.BLACK) { /* shift is visual for this starter */ }, weighted(1.25f, 54))
                }
                row.forEachIndexed { index, ch ->
                    val c = colorFor(ch.toString() + index)
                    line.addView(key(ch.toString(), c, textFor(c)) { press(ch.toString()) }, weighted(1f, 54))
                }
                if (idx == 2) line.addView(key("⌫", cream, Color.BLACK) { press("⌫") }, weighted(1.25f, 54))
                container.addView(line)
            }

            val bottom = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            bottom.addView(key("?123", red, Color.WHITE) { symbols = true; refresh() }, weighted(1.25f, 54))
            bottom.addView(key("🌐", blue, Color.WHITE) { cycleMode() }, weighted(0.9f, 54))
            bottom.addView(key(",", cream, Color.BLACK) { press(",") }, weighted(0.7f, 54))
            bottom.addView(key("English / বাংলা", dark, Color.WHITE) { cycleMode() }, weighted(3.8f, 54))
            bottom.addView(key(".", dark, Color.WHITE) { press(".") }, weighted(0.7f, 54))
            bottom.addView(key("↵", red, Color.WHITE) { press("ENTER") }, weighted(1.1f, 54))
            container.addView(bottom)
        }
        return container
    }

    private fun key(label: String, color: Int, textColor: Int, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = dotted(label)
            textSize = if (label.length > 3) 13f else 21f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(15).toFloat()
                setStroke(dp(1), Color.argb(35, 255, 255, 255))
            }
            setPadding(dp(2), dp(3), dp(2), dp(3))
            val lp = LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
            layoutParams = lp
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun dotted(s: String): String {
        if (s.length > 3 || s.any { it.code > 127 }) return s
        return s.toCharArray().joinToString(" ") { it.toString() }
    }

    private fun colorFor(seed: String): Int = when ((seed.lastOrNull()?.code ?: 0) % 5) {
        0 -> yellow
        1 -> blue
        2 -> dark
        3 -> cream
        else -> red
    }

    private fun textFor(color: Int) = if (color == cream || color == yellow) Color.BLACK else Color.WHITE

    private fun modeLabel() = when (mode) {
        Mode.ENGLISH -> "EN"
        Mode.PHONETIC -> "বাংলা • Ph"
        Mode.BANGLA -> "বাংলা"
    }

    private fun cycleMode() {
        mode = when (mode) {
            Mode.ENGLISH -> Mode.PHONETIC
            Mode.PHONETIC -> Mode.BANGLA
            Mode.BANGLA -> Mode.ENGLISH
        }
        phoneticBuffer = ""
        refresh()
    }

    private fun press(value: String) {
        val ic = currentInputConnection ?: return
        when (value) {
            "⌫" -> {
                if (mode == Mode.PHONETIC && phoneticBuffer.isNotEmpty()) {
                    phoneticBuffer = phoneticBuffer.dropLast(1)
                    ic.setComposingText(BanglaPhonetic.convert(phoneticBuffer), 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            " " -> {
                if (mode == Mode.PHONETIC && phoneticBuffer.isNotEmpty()) {
                    ic.commitText(BanglaPhonetic.convert(phoneticBuffer), 1)
                    phoneticBuffer = ""
                }
                ic.commitText(" ", 1)
            }
            "ENTER" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_SEARCH ||
                    action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_DONE) {
                    ic.performEditorAction(action!!)
                } else ic.commitText("\n", 1)
            }
            else -> {
                if (mode == Mode.PHONETIC && value.length == 1 && value[0].isLetter()) {
                    phoneticBuffer += value.lowercase()
                    ic.setComposingText(BanglaPhonetic.convert(phoneticBuffer), 1)
                } else {
                    if (mode == Mode.PHONETIC && phoneticBuffer.isNotEmpty()) {
                        ic.commitText(BanglaPhonetic.convert(phoneticBuffer), 1)
                        phoneticBuffer = ""
                    }
                    ic.commitText(if (mode == Mode.ENGLISH) value.lowercase() else value, 1)
                }
            }
        }
        refresh()
    }

    private fun refresh() {
        val old = root
        val view = buildKeyboard()
        if (old != null && old.parent is ViewGroup) {
            (old.parent as ViewGroup).removeView(old)
        }
        setInputView(view)
    }

    private fun weighted(weight: Float, heightDp: Int) =
        LinearLayout.LayoutParams(0, dp(heightDp), weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
