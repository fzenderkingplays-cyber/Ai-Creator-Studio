package com.aicreatorstudio.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale
import java.net.URL
import java.net.HttpURLConnection
import java.util.UUID
import android.graphics.BitmapFactory
import android.widget.ImageView
import org.json.JSONObject
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private var language = "en"
    private var mode = 0
    private var promptText = ""
    private var selectedStyle = 0
    // Set this to your own HTTPS backend URL before building.
    private val backendUrl = "https://YOUR-SERVER.example.com"
    private val deviceId by lazy {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }
    }

    private fun tr(en: String, ur: String) = if (language == "ur") ur else en

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        language = getSharedPreferences("settings", MODE_PRIVATE).getString("language", "en") ?: "en"
        mode = savedInstanceState?.getInt("mode") ?: 0
        promptText = savedInstanceState?.getString("prompt") ?: ""
        selectedStyle = savedInstanceState?.getInt("style") ?: 0
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("mode", mode)
        outState.putString("prompt", promptText)
        outState.putInt("style", selectedStyle)
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        val rtl = language == "ur"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 28)
            setBackgroundColor(Color.rgb(13, 17, 31))
            layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        fun label(text: String, size: Float) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 12)
            textDirection = if (rtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        }
        root.addView(label("✨ AI Creator Studio", 27f).apply { setTypeface(null, Typeface.BOLD) })
        root.addView(label(tr("Create images & videos with AI", "اے آئی سے تصاویر اور ویڈیوز بنائیں"), 16f))

        val languageRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        languageRow.addView(label(tr("Language", "زبان"), 15f))
        val languageSpinner = Spinner(this)
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("English", "اردو"))
        languageSpinner.setSelection(if (rtl) 1 else 0)
        languageRow.addView(languageSpinner)
        root.addView(languageRow)

        root.addView(label(tr("Generation mode", "تخلیق کی قسم"), 17f))
        val modes = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val modeLabels = listOf(tr("Text to Image", "متن سے تصویر"), tr("Text to Video", "متن سے ویڈیو"), tr("Image to Video (coming soon)", "تصویر سے ویڈیو (جلد آرہا ہے)"))
        modeLabels.forEachIndexed { i, name ->
            modes.addView(RadioButton(this).apply { text = name; setTextColor(Color.WHITE); id = i + 1; textSize = 15f })
        }
        modes.check(mode + 1)
        modes.setOnCheckedChangeListener { _, checked -> mode = checked - 1 }
        root.addView(modes)

        root.addView(label(tr("Your prompt", "آپ کا پرامپٹ"), 17f))
        val prompt = EditText(this).apply {
            hint = tr("A cute carrot walking in a colorful 3D village...", "ایک پیاری گاجر رنگین تھری ڈی گاؤں میں چل رہی ہے...")
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            minLines = 4
            gravity = Gravity.TOP
            setBackgroundColor(Color.rgb(35, 43, 67))
            setPadding(16, 16, 16, 16)
            setText(promptText)
            textDirection = if (rtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        }
        root.addView(prompt)

        root.addView(label(tr("Style", "اسٹائل"), 17f))
        val style = Spinner(this)
        val styles = listOf(tr("3D Cartoon", "تھری ڈی کارٹون"), tr("Anime", "اینیمی"), tr("Realistic", "حقیقت پسندانہ"), tr("Cinematic", "سنیماٹک"))
        style.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, styles)
        style.setSelection(selectedStyle)
        root.addView(style)

        val status = label(tr("Ready to create", "تخلیق کے لیے تیار"), 15f)
        val preview = ImageView(this).apply { adjustViewBounds = true; visibility = View.GONE }
        val generate = Button(this).apply { text = tr("✨ Generate", "✨ بنائیں") }
        generate.setOnClickListener {
            promptText = prompt.text.toString().trim()
            selectedStyle = style.selectedItemPosition
            if (promptText.isBlank()) { status.text = tr("Please enter a prompt.", "براہ کرم پرامپٹ لکھیں۔"); return@setOnClickListener }
            if (mode == 2) { status.text = tr("Image to video is coming soon.", "تصویر سے ویڈیو جلد آ رہی ہے۔"); return@setOnClickListener }
            if (backendUrl.contains("YOUR-SERVER")) { status.text = tr("Configure your HTTPS server URL first.", "پہلے اپنے HTTPS سرور کا پتہ درج کریں۔"); return@setOnClickListener }
            generate.isEnabled = false
            status.text = tr("Generating, please wait...", "تیار ہو رہا ہے، انتظار کریں...")
            preview.visibility = View.GONE
            val selectedMode = if (mode == 0) "image" else "video"
            val chosenPrompt = promptText
            val chosenStyle = listOf("3D Cartoon", "Anime", "Realistic", "Cinematic")[selectedStyle]
            Thread {
                try {
                    val conn = URL("$backendUrl/generate").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 20000
                    conn.readTimeout = 200000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("X-Device-Id", deviceId)
                    val json = JSONObject().put("mode", selectedMode).put("prompt", chosenPrompt).put("style", chosenStyle)
                    conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    if (code != 200) {
                        val message = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                        throw Exception(JSONObject(message).optString("error", "HTTP $code"))
                    }
                    val ext = if (selectedMode == "image") "png" else "mp4"
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ai_${System.currentTimeMillis()}.$ext")
                    conn.inputStream.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
                    conn.disconnect()
                    runOnUiThread {
                        if (selectedMode == "image") {
                            preview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                            preview.visibility = View.VISIBLE
                        }
                        status.text = tr("Saved to app files: ", "ایپ فائلوں میں محفوظ: ") + file.absolutePath
                        generate.isEnabled = true
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        status.text = tr("Generation failed: ", "تخلیق ناکام: ") + (e.message ?: "Unknown error")
                        generate.isEnabled = true
                    }
                }
            }.start()
        }
        root.addView(generate)
        root.addView(status)
        root.addView(preview)
        root.addView(label(tr("Free demo • Daily limits enforced by server • No API keys in app", "مفت آزمائشی ورژن • روزانہ حد سرور پر • ایپ میں کوئی اے پی آئی کلید نہیں"), 12f))

        languageSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newLanguage = if (position == 1) "ur" else "en"
                if (newLanguage != language) {
                    promptText = prompt.text.toString()
                    selectedStyle = style.selectedItemPosition
                    language = newLanguage
                    getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", language).apply()
                    render()
                }
            }
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
