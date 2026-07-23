package com.jarvis.assistant

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            setBackgroundColor(0xFF0A0A0F.toInt())
        }

        val label = TextView(this).apply {
            text = "Enter your AI API key"
            setTextColor(0xFF00E5FF.toInt())
            textSize = 18f
        }

        val input = EditText(this).apply {
            hint = "sk-ant-..."
            setText(prefs.getString("api_key", ""))
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
        }

        val saveButton = Button(this).apply {
            text = "Save"
            setOnClickListener {
                val key = input.text.toString().trim()
                prefs.edit().putString("api_key", key).apply()
                Toast.makeText(this@SettingsActivity, "Saved.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        layout.addView(label)
        layout.addView(input)
        layout.addView(saveButton)
        setContentView(layout)
    }
}
