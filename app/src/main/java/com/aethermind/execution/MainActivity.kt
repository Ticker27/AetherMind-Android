package com.aethermind.execution

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "AetherMind Execution Core"
            textSize = 20f
        }

        val status = TextView(this).apply {
            text = "Open Accessibility Settings and enable AetherMind Execution Service before dispatching commands."
            textSize = 14f
        }

        val settingsButton = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val stopButton = Button(this).apply {
            text = "Emergency Stop"
            setOnClickListener {
                AetherRuntime.emergencyStop()
                status.text = "Emergency stop sent. Native queue cleared."
            }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                addView(title)
                addView(status)
                addView(settingsButton)
                addView(stopButton)
            }
        )
    }
}
