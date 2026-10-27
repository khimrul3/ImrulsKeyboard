package com.imrulskeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.media.AudioManager
import android.view.Gravity
import android.view.View
import android.view.animation.ScaleAnimation
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

/** Responsive English IME with RGB mechanical styling and selectable palettes. */
class KeyboardService : InputMethodService() {
    private var symbols = false
    private var shifted = false
    private var capsLock = false
    private var listening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var suggestionRow: LinearLayout? = null
    private var statusText: TextView? = null

    private data class Theme(val name: String, val base: Int, val key: Int, val accent: Int, val text: Int, val alternate: Int)
    private val themes = listOf(
        Theme("Neon Blue",0xFF070C16.toInt(),0xFF17263A.toInt(),0xFF00D9FF.toInt(),Color.WHITE,0xFF174B83.toInt()),
        Theme("Cyber Purple",0xFF0C0717.toInt(),0xFF251638.toInt(),0xFFC77DFF.toInt(),Color.WHITE,0xFF6332A0.toInt()),
        Theme("RGB Rainbow",0xFF090A12.toInt(),0xFF20202C.toInt(),0xFFFF3D81.toInt(),Color.WHITE,0xFF159CC8.toInt()),
        Theme("Red Samurai",0xFF100709.toInt(),0xFF30141A.toInt(),0xFFFF344D.toInt(),Color.WHITE,0xFF8E1F30.toInt()),
        Theme("Carbon Black",0xFF050607.toInt(),0xFF191D22.toInt(),0xFF9BA8B7.toInt(),Color.WHITE,0xFF353C45.toInt()),
        Theme("Frost White",0xFFE4EAF3.toInt(),0xFFF9FBFF.toInt(),0xFF4774B7.toInt(),0xFF101722.toInt(),0xFFB8CBE6.toInt()),
        Theme("Ocean Wave",0xFF041522.toInt(),0xFF0D2B40.toInt(),0xFF26C6FF.toInt(),Color.WHITE,0xFF12618A.toInt()),
        Theme("Gold Luxury",0xFF120E08.toInt(),0xFF302414.toInt(),0xFFFFD36A.toInt(),Color.WHITE,0xFF79541C.toInt()),
        Theme("Sakura Pink",0xFF1B1019.toInt(),0xFF392034.toInt(),0xFFFF92CC.toInt(),Color.WHITE,0xFF923E75.toInt()),
        Theme("Forest Green",0xFF06130C.toInt(),0xFF14291D.toInt(),0xFF46E69B.toInt(),Color.WHITE,0xFF176A48.toInt()),
        Theme("Sunset Orange",0xFF1A0E09.toInt(),0xFF382015.toInt(),0xFFFF9C43.toInt(),Color.WHITE,0xFFB74A23.toInt()),
        Theme("Glacier",0xFF09151E.toInt(),0xFF1B303E.toInt(),0xFFA5EDFF.toInt(),Color.WHITE,0xFF477F9A.toInt()),
        Theme("Matrix",0xFF020A04.toInt(),0xFF0C2112.toInt(),0xFF38FF78.toInt(),Color.WHITE,0xFF145D2B.toInt()),
        Theme("Violet Storm",0xFF0E0920.toInt(),0xFF251A44.toInt(),0xFF9A83FF.toInt(),Color.WHITE,0xFF5136A8.toInt()),
        Theme("Stealth Grey",0xFF111317.toInt(),0xFF252A31.toInt(),0xFFBBC3CE.toInt(),Color.WHITE,0xFF444B55.toInt()),
        Theme("Holographic",0xFF0D1019.toInt(),0xFF20283B.toInt(),0xFF7EEBFF.toInt(),Color.WHITE,0xFFD77AFF.toInt()),
        Theme("Toxic Lime",0xFF101408.toInt(),0xFF252D12.toInt(),0xFFD7FF47.toInt(),Color.WHITE,0xFF647D17.toInt()),
        Theme("Wood Retro",0xFF1B120C.toInt(),0xFF3A281A.toInt(),0xFFD8A06B.toInt(),Color.WHITE,0xFF80502F.toInt()),
        Theme("Midnight Blue",0xFF050A19.toInt(),0xFF131F3D.toInt(),0xFF5B8DFF.toInt(),Color.WHITE,0xFF263D8B.toInt()),
        Theme("AMOLED Dark",Color.BLACK,0xFF111111.toInt(),Color.WHITE,Color.WHITE,0xFF292929.toInt()),
        Theme("Mint Tech",0xFF061412.toInt(),0xFF17302C.toInt(),0xFF67FFD1.toInt(),Color.WHITE,0xFF217B69.toInt()),
        Theme("Electric Blue",0xFF050B18.toInt(),0xFF10294B.toInt(),0xFF388BFF.toInt(),Color.WHITE,0xFF2056A0.toInt())
    )
    private val prefs by lazy { getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE) }
    private val theme: Theme get() = themes[prefs.getInt("theme", 0).coerceIn(themes.indices)]

    override fun onCreateInputView(): View = buildKeyboard()
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = capsLock
    }
    override fun onDestroy() { stopVoice(); super.onDestroy() }

    private fun buildKeyboard(): View {
        val t = theme
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.base)
            val bgUri = prefs.getString("background_uri", null)
            if (!bgUri.isNullOrBlank()) try { contentResolver.openInputStream(Uri.parse(bgUri))?.use { stream -> background = BitmapDrawable(resources, BitmapFactory.decodeStream(stream)) } } catch (_: Exception) {}
            setPadding(dp(4), dp(5), dp(4), dp(7))
            clipChildren = false; clipToPadding = false
        }
        val top = row()
        top.addView(key("IMRULS  RGB", t.base, t.accent, 34, 12f, false) { showThemes() }, weighted(2.1f, 34))
        top.addView(key(if (listening) "Listening…" else "🎙", t.key, t.accent, 34, 18f) { toggleVoice() }, weighted(0.9f, 34))
        top.addView(key("⚙", t.key, t.text, 34, 18f) { showThemes() }, weighted(0.9f, 34))
        container.addView(top, LinearLayout.LayoutParams(-1, dp(39)))
        addGap(container, 2)

        val suggestions = row().apply { gravity = Gravity.CENTER; setPadding(dp(2), 0, dp(2), 0) }
        suggestionRow = suggestions
        container.addView(suggestions, LinearLayout.LayoutParams(-1, dp(34)))
        updateSuggestions("")
        addGap(container, 1)

        if (!symbols) {
            addLetterRow(container, "QWERTYUIOP", 0, 1f)
            addLetterRow(container, "ASDFGHJKL", 1, 0.90f)
            val third = row()
            third.addView(key(if (shifted) "⇧" else "⇧", t.alternate, t.accent, 43, 20f) { shifted = !shifted; capsLock = shifted; rebuild() }, weighted(1.35f,43))
            "ZXCVBNM".forEachIndexed { i,ch -> third.addView(letterKey(ch, i+2), weighted(1f,43)) }
            third.addView(key("⌫", t.alternate, t.text, 43, 19f) { delete() }, weighted(1.35f,43))
            container.addView(third)
            val bottom = row()
            bottom.addView(key("?123", t.alternate, t.text, 44, 13f) { symbols = true; rebuild() }, weighted(1.25f,44))
            bottom.addView(key("☺", t.key, t.accent, 44, 19f) { commit("😊") }, weighted(0.85f,44))
            bottom.addView(key(",", t.key, t.text, 44, 18f) { commit(",") }, weighted(0.7f,44))
            bottom.addView(key("English (US)", t.key, t.text, 44, 11f) { commit(" ") }, weighted(4.0f,44))
            bottom.addView(key(".", t.key, t.text, 44, 18f) { commit(".") }, weighted(0.7f,44))
            bottom.addView(key("↵", t.alternate, t.accent, 44, 19f) { enter() }, weighted(1.25f,44))
            container.addView(bottom)
        } else {
            listOf("1234567890", "@#\$%&-*+/", "()[]{}!?;:").forEachIndexed { ri, chars ->
                val line = row()
                chars.forEachIndexed { i,ch -> line.addView(key(ch.toString(), t.key, t.text, 43, 18f) { commit(ch.toString()) }, weighted(1f,43)) }
                container.addView(line)
            }
            val bottom = row()
            bottom.addView(key("ABC", t.alternate, t.text, 44, 13f) { symbols=false; rebuild() }, weighted(1.3f,44))
            bottom.addView(key("Space", t.key, t.text, 44, 12f) { commit(" ") }, weighted(4f,44))
            bottom.addView(key("⌫", t.alternate, t.text, 44, 18f) { delete() }, weighted(1.3f,44))
            container.addView(bottom)
        }
        return container
    }

    private fun addLetterRow(container: LinearLayout, letters: String, rowIndex: Int, unused: Float) {
        val line = row()
        if (rowIndex == 1) line.setPadding(dp(12), 0, dp(12), 0)
        letters.forEachIndexed { i,ch -> line.addView(letterKey(ch, i+rowIndex), weighted(1f,43)) }
        container.addView(line)
    }
    private fun letterKey(ch: Char, index: Int): TextView {
        val t = theme
        val label = if (shifted) ch.uppercaseChar().toString() else ch.lowercaseChar().toString()
        val colors = listOf(t.key, t.alternate, t.key, t.key, t.alternate)
        val color = colors[(ch.code + index) % colors.size]
        return key(label, color, t.text, 43, 20f) { commit(label); if (shifted && !capsLock) { shifted=false; rebuild() } }
    }
    private fun row() = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; clipChildren=false; clipToPadding=false }
    private fun key(label: String, color: Int, textColor: Int, height: Int, size: Float=19f, outlined: Boolean=true, action: () -> Unit): TextView {
        val t = theme
        return TextView(this).apply {
            text=label; textSize=size; gravity=Gravity.CENTER; setTextColor(textColor)
            typeface=Typeface.create("sans-serif-condensed", Typeface.BOLD); includeFontPadding=false; maxLines=1
            setPadding(dp(1),0,dp(1),0)
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(lighten(color),color, darken(color))).apply {
                cornerRadius=dp(9).toFloat(); setStroke(dp(if(outlined) 1 else 0), if(outlined) Color.argb(130, t.accent) else color)
            }
            elevation=dp(2).toFloat(); isClickable=true; isFocusable=true
            contentDescription=if(label.isBlank()) "Space" else label
            setOnClickListener {
                startAnimation(ScaleAnimation(1f,0.94f,1f,0.94f,ScaleAnimation.RELATIVE_TO_SELF,0.5f,ScaleAnimation.RELATIVE_TO_SELF,0.5f).apply { duration=65; fillAfter=true })
                if (prefs.getBoolean("sound", false)) try { (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.35f) } catch (_: Exception) {}
                if (prefs.getBoolean("vibration", false)) vibrate()
                action()
            }
        }
    }
    private fun weighted(weight: Float, h: Int) = LinearLayout.LayoutParams(0,dp(h),weight).apply { setMargins(dp(2),dp(3),dp(2),dp(3)) }
    private fun addGap(parent: LinearLayout, h: Int) { parent.addView(View(this),LinearLayout.LayoutParams(1,dp(h))) }
    private fun commit(s: String) { currentInputConnection?.commitText(s,1); if (s.length == 1 && s[0].isLetter()) updateSuggestions(s) }
    private fun delete() { currentInputConnection?.deleteSurroundingText(1,0); updateSuggestions("") }
    private fun enter() { val ic=currentInputConnection ?: return; val a=currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION); if(a!=null && a!=EditorInfo.IME_ACTION_NONE && a!=EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(a) else ic.commitText("\n",1) }
    private fun rebuild() { setInputView(buildKeyboard()) }
    private fun dp(v: Int)=(v*resources.displayMetrics.density+0.5f).toInt()
    private fun lighten(c:Int):Int=Color.rgb((Color.red(c)*1.22f).toInt().coerceAtMost(255),(Color.green(c)*1.22f).toInt().coerceAtMost(255),(Color.blue(c)*1.22f).toInt().coerceAtMost(255))
    private fun darken(c:Int):Int=Color.rgb((Color.red(c)*0.55f).toInt(),(Color.green(c)*0.55f).toInt(),(Color.blue(c)*0.55f).toInt())
    private fun vibrate() { try { if (Build.VERSION.SDK_INT >= 31) { (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator.vibrate(VibrationEffect.createOneShot(12,35)) } else { @Suppress("DEPRECATION") val v = getSystemService(VIBRATOR_SERVICE) as Vibrator; @Suppress("DEPRECATION") v.vibrate(12) } } catch (_:Exception){} }

    private fun updateSuggestions(prefix: String) {
        val target = suggestionRow ?: return
        target.removeAllViews()
        val t=theme
        val candidates = if(prefix.isBlank()) listOf("✦ Themes", "🎙 Voice", "⚙ Settings") else {
            val p=prefix.lowercase()
            val dict=mapOf("hel" to listOf("hello","help","held"), "tha" to listOf("thanks","thank","that"), "goo" to listOf("good","google","going"), "how" to listOf("how","however","house"), "im" to listOf("I'm","important","improve"), "wh" to listOf("what","when","where"), "you" to listOf("you","your","you're"), "the" to listOf("the","there","their"))
            dict.entries.firstOrNull { p.startsWith(it.key) }?.value ?: listOf(prefix, prefix+"s", prefix+"ing")
        }
        candidates.take(3).forEachIndexed { i,s ->
            val label=TextView(this).apply { text=s; textSize=12f; gravity=Gravity.CENTER; setTextColor(t.text); maxLines=1; background=GradientDrawable().apply { setColor(if(i==0) t.alternate else t.key); cornerRadius=dp(7).toFloat(); setStroke(dp(1),Color.argb(80,t.accent)) }; setOnClickListener { if(s=="✦ Themes" || s=="⚙ Settings") showThemes() else if(s=="🎙 Voice") toggleVoice() else { currentInputConnection?.deleteSurroundingText(32,0); commit(s+" ") } } }
            target.addView(label, LinearLayout.LayoutParams(0,dp(29),1f).apply { setMargins(dp(2),dp(1),dp(2),dp(1)) })
        }
    }
    private fun showThemes() {
        val names=themes.mapIndexed { i,t -> "${i+1}. ${t.name}" }.toTypedArray()
        android.app.AlertDialog.Builder(this).setTitle("RGB Mechanical Themes").setItems(names) { _, which -> prefs.edit().putInt("theme",which).apply(); rebuild() }
            .setNeutralButton(if(prefs.getBoolean("sound",false)) "Sound: ON" else "Sound: OFF") { _,_ -> prefs.edit().putBoolean("sound",!prefs.getBoolean("sound",false)).apply(); rebuild() }
            .setNegativeButton(if(prefs.getBoolean("vibration",false)) "Vibration: ON" else "Vibration: OFF") { _,_ -> prefs.edit().putBoolean("vibration",!prefs.getBoolean("vibration",false)).apply(); rebuild() }.show()
    }
    private fun toggleVoice() {
        if (listening) { stopVoice(); rebuild(); return }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { statusText?.text="Speech recognition unavailable"; return }
        try {
            listening=true
            speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
                sr.setRecognitionListener(object: RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { }
                    override fun onBeginningOfSpeech() { }
                    override fun onRmsChanged(rmsdB: Float) { }
                    override fun onBufferReceived(buffer: ByteArray?) { }
                    override fun onEndOfSpeech() { listening=false; rebuild() }
                    override fun onError(error: Int) { listening=false; rebuild(); stopVoice() }
                    override fun onResults(results: android.os.Bundle?) { val words=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(!words.isNullOrEmpty()) commit(words[0]); listening=false; rebuild(); stopVoice() }
                    override fun onPartialResults(partialResults: android.os.Bundle?) { val words=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(!words.isNullOrEmpty()) { suggestionRow?.removeAllViews(); val v=TextView(this@KeyboardService).apply { text="🎙 ${words[0]}"; setTextColor(theme.accent); gravity=Gravity.CENTER; textSize=12f }; suggestionRow?.addView(v,LinearLayout.LayoutParams(-1,dp(29))) } }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) { }
                })
                val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US"); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3) }
                sr.startListening(intent)
            }
            rebuild()
        } catch (_: Exception) { listening=false; stopVoice(); rebuild() }
    }
    private fun stopVoice() { try { speechRecognizer?.stopListening(); speechRecognizer?.destroy() } catch (_:Exception){}; speechRecognizer=null; listening=false }
}
