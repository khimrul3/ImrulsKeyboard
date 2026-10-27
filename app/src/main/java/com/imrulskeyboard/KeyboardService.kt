package com.imrulskeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

/** English-only, responsive keyboard. */
class KeyboardService : InputMethodService() {
    private var symbols = false
    private var shifted = false

    private val black = Color.rgb(0, 0, 0)
    private val dark = Color.rgb(25, 28, 32)
    private val cream = Color.rgb(244, 237, 233)
    private val yellow = Color.rgb(239, 190, 43)
    private val blue = Color.rgb(43, 91, 151)
    private val red = Color.rgb(198, 48, 49)

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = false
    }

    private fun buildKeyboard(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(black)
            setPadding(dp(3), dp(6), dp(3), dp(7))
        }

        // Minimal top bar, like the reference design.
        val top = row()
        top.addView(key("›", dark, Color.WHITE, 31, 0.75f) { requestHideSelf(0) }, weighted(0.75f, 31))
        top.addView(spaceView(), LinearLayout.LayoutParams(0, dp(31), 5.5f))
        top.addView(key("◎", dark, Color.WHITE, 31, 0.75f) { /* English-only */ }, weighted(0.75f, 31))
        container.addView(top, LinearLayout.LayoutParams(-1, dp(37)))
        addGap(container, 4)

        if (!symbols) {
            addLetterRow(container, "QWERTYUIOP", 1)
            addLetterRow(container, "ASDFGHJKL", 2)
            val third = row()
            third.addView(key("⇧", yellow, Color.BLACK, 49, 0.95f) { shifted = !shifted; rebuild() }, weighted(1.45f, 49))
            "ZXCVBNM".forEachIndexed { i, ch ->
                val c = colorFor(ch, i)
                third.addView(key(letterLabel(ch), c, textFor(c), 49) { typeLetter(ch) }, weighted(1f, 49))
            }
            third.addView(key("⌫", cream, Color.BLACK, 49, 0.9f) { delete() }, weighted(1.45f, 49))
            container.addView(third)

            val bottom = row()
            bottom.addView(key("?123", red, Color.WHITE, 49, 13f) { symbols = true; rebuild() }, weighted(1.35f, 49))
            bottom.addView(key("☻", blue, Color.WHITE, 49, 21f) { /* decorative emoji key */ }, weighted(1f, 49))
            bottom.addView(key(",", cream, Color.BLACK, 49, 20f) { commit(",") }, weighted(0.75f, 49))
            bottom.addView(key("", dark, Color.WHITE, 49, 20f) { commit(" ") }, weighted(4.15f, 49))
            bottom.addView(key(".", dark, Color.WHITE, 49, 20f) { commit(".") }, weighted(0.75f, 49))
            bottom.addView(key("↵", red, Color.WHITE, 49, 21f) { enter() }, weighted(1.35f, 49))
            container.addView(bottom)
        } else {
            val rows = listOf("1234567890", "@#\$%&-*+/", "()[]{}!?;:")
            rows.forEachIndexed { ri, chars ->
                val line = row()
                chars.forEachIndexed { i, ch ->
                    val c = colorFor(ch, i + ri)
                    line.addView(key(ch.toString(), c, textFor(c), 49) { commit(ch.toString()) }, weighted(1f, 49))
                }
                container.addView(line)
            }
            val bottom = row()
            bottom.addView(key("ABC", red, Color.WHITE, 49, 14f) { symbols = false; rebuild() }, weighted(1.4f, 49))
            bottom.addView(key("", dark, Color.WHITE, 49) { commit(" ") }, weighted(4f, 49))
            bottom.addView(key("⌫", cream, Color.BLACK, 49) { delete() }, weighted(1.4f, 49))
            container.addView(bottom)
        }
        addGap(container, 3)
        return container
    }

    private fun addLetterRow(container: LinearLayout, letters: String, rowIndex: Int) {
        val line = row()
        if (rowIndex == 2) line.setPadding(dp(17), 0, dp(17), 0)
        letters.forEachIndexed { index, ch ->
            val c = colorFor(ch, index + rowIndex)
            line.addView(key(letterLabel(ch), c, textFor(c), 49) { typeLetter(ch) }, weighted(1f, 49))
        }
        container.addView(line)
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        clipChildren = false
        clipToPadding = false
    }

    private fun spaceView() = View(this)

    private fun key(label: String, color: Int, textColor: Int, height: Int, textSize: Float = 21f, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            this.textSize = textSize
            gravity = Gravity.CENTER
            setTextColor(textColor)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            includeFontPadding = false
            maxLines = 1
            setPadding(dp(1), 0, dp(1), 0)
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(13).toFloat()
                setStroke(dp(1), Color.argb(35, 255, 255, 255))
            }
            isClickable = true
            isFocusable = true
            contentDescription = if (label.isBlank()) "Space" else label
            setOnClickListener { action() }
        }
    }

    private fun weighted(weight: Float, heightDp: Int) = LinearLayout.LayoutParams(0, dp(heightDp), weight).apply {
        setMargins(dp(2), dp(3), dp(2), dp(3))
    }

    private fun addGap(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun colorFor(ch: Char, index: Int): Int = when ((ch.code + index) % 5) {
        0 -> yellow
        1 -> blue
        2 -> dark
        3 -> cream
        else -> red
    }

    private fun textFor(color: Int) = if (color == cream || color == yellow) Color.BLACK else Color.WHITE
    private fun letterLabel(ch: Char) = ch.toString()

    private fun typeLetter(ch: Char) {
        commit(if (shifted) ch.toString() else ch.lowercaseChar().toString())
        if (shifted) { shifted = false; rebuild() }
    }

    private fun commit(text: String) { currentInputConnection?.commitText(text, 1) }

    private fun delete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun enter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }

    private fun rebuild() { setInputView(buildKeyboard()) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
