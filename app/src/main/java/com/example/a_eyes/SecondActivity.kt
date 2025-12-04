package com.example.a_eyes

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {

    private val languages = listOf("English", "Français")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val spinner = findViewById<Spinner>(R.id.languageSpinner)
        val backBtn = findViewById<ImageButton>(R.id.btnBack)

        // Adapter for Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Pre-select current language
        val currentLang = intent.getBooleanExtra("currentLang", true) // true = French
        spinner.setSelection(if (currentLang) 1 else 0)

        // Back button sends result to MainActivity
        backBtn.setOnClickListener {
            val selectedLang = spinner.selectedItemPosition == 1 // French = position 1
            val resultIntent = Intent()
            resultIntent.putExtra("selectedLang", selectedLang)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
