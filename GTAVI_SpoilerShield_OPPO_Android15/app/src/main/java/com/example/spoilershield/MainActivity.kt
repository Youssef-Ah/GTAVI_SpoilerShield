package com.example.spoilershield

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("shield", MODE_PRIVATE) }
    private lateinit var facebook: CheckBox
    private lateinit var youtube: CheckBox
    private lateinit var keywords: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (18 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        layout.addView(TextView(this).apply {
            text = "GTA VI Spoiler Shield"
            textSize = 24f
        })
        layout.addView(TextView(this).apply {
            text = "OPPO / realme Android 15 build. It checks visible accessibility text in selected apps. On a match, it attempts a controlled upward swipe to move the matching feed card out of view. It does not delete posts or guarantee perfect card-level skipping."
            textSize = 14f
            setPadding(0, pad / 2, 0, pad)
        })

        facebook = CheckBox(this).apply {
            text = "Filter Facebook"
            isChecked = prefs.getBoolean("facebook_enabled", true)
        }
        youtube = CheckBox(this).apply {
            text = "Filter YouTube"
            isChecked = prefs.getBoolean("youtube_enabled", true)
        }
        layout.addView(facebook)
        layout.addView(youtube)

        layout.addView(TextView(this).apply {
            text = "Blocked keywords (one per line):"
            setPadding(0, pad, 0, 4)
        })

        keywords = EditText(this).apply {
            minLines = 8
            gravity = Gravity.TOP
            setText(prefs.getString("keywords", DEFAULT_KEYWORDS))
            hint = "GTA VI"
        }
        layout.addView(keywords, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        layout.addView(Button(this).apply {
            text = "Save settings"
            setOnClickListener {
                prefs.edit()
                    .putBoolean("facebook_enabled", facebook.isChecked)
                    .putBoolean("youtube_enabled", youtube.isChecked)
                    .putString("keywords", keywords.text.toString())
                    .apply()
                Toast.makeText(this@MainActivity, "Settings saved", Toast.LENGTH_SHORT).show()
            }
        })

        layout.addView(Button(this).apply {
            text = "Enable filtering (Accessibility)"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Accessibility permission")
                    .setMessage(
                        "This service can read visible interface text in Facebook and YouTube and perform a swipe when a blocked keyword is found. Text is processed locally by this app and is not uploaded. It may skip nearby content or miss text that an app does not expose to accessibility. Enable only if you understand and accept this."
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue") { _, _ ->
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .show()
            }
        })

        layout.addView(TextView(this).apply {
            text = "Keep keywords specific. Recommended: GTA VI, GTA 6, GTA6, Grand Theft Auto VI, Grand Theft Auto 6, Lucia GTA, Jason GTA. Avoid broad terms like Rockstar or GTA alone."
            textSize = 12f
            setPadding(0, pad, 0, 0)
        })

        setContentView(layout)
    }

    companion object {
        const val DEFAULT_KEYWORDS =
            "GTA VI\nGTA 6\nGTA6\nGrand Theft Auto VI\nGrand Theft Auto 6\nLucia GTA\nJason GTA"
    }
}
