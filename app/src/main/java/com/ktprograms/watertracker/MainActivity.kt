/*
 * GNU General Public License v3.0
 *
 * Copyright (c) 2021 Toh Jeen Gie Keith
 *
 *
 * This file is part of WaterTracker.
 *
 * WaterTracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WaterTracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WaterTracker.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ktprograms.watertracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    // View references
    private val glass1: ImageButton by lazy { findViewById(R.id.glass1) }
    private val glass2: ImageButton by lazy { findViewById(R.id.glass2) }
    private val glass3: ImageButton by lazy { findViewById(R.id.glass3) }
    private val glass4: ImageButton by lazy { findViewById(R.id.glass4) }
    private val glass5: ImageButton by lazy { findViewById(R.id.glass5) }
    private val okBtn: Button by lazy { findViewById(R.id.okBtn) }
    private val resetBtn: Button by lazy { findViewById(R.id.resetBtn) }
    private val timeLinearLayout: LinearLayout by lazy { findViewById(R.id.timeLinearLayout) }
    private val congratsLinearLayout: LinearLayout by lazy { findViewById(R.id.congratsLinearLayout) }
    private val minsEditText: EditText by lazy { findViewById(R.id.minsEditText) }
    private val alarmTextView: TextView by lazy { findViewById(R.id.alarmTextView) }

    // Initialize arrays
    private val glassAmounts = mutableListOf(0, 0, 0, 0, 0) // 0 -> full, 4 -> empty
    private val glassImages = listOf(
        R.drawable.glass_100,
        R.drawable.glass_75,
        R.drawable.glass_50,
        R.drawable.glass_25,
        R.drawable.glass_0
    )

    // Other object references
    private val prefs: SharedPreferences by lazy { getSharedPreferences("Prefs", MODE_PRIVATE) }
    private val alarmManager: AlarmManager by lazy { getSystemService(ALARM_SERVICE) as AlarmManager }
    private val alarmIntent: Intent by lazy { Intent(applicationContext, AlarmReceiver::class.java) }
    private val pendingIntent: PendingIntent by lazy { PendingIntent.getBroadcast(applicationContext, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE) }

    // Other variables / constants
    private var wait = 0
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Put the app icon in the app bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.drawable.app_icon)
        supportActionBar?.setDisplayUseLogoEnabled(true)

        // Create a notification channel (For Android O [28] and above only)
        createNotificationChannel()

        // Read the shared preferences
        readPrefs()

        // Set glass onClick listeners
        glass1.setOnClickListener {
            glassClicked(0)
        }
        glass2.setOnClickListener {
            glassClicked(1)
        }
        glass3.setOnClickListener {
            glassClicked(2)
        }
        glass4.setOnClickListener {
            glassClicked(3)
        }
        glass5.setOnClickListener {
            glassClicked(4)
        }

        // Set Ok and Reset Button onClick listeners
        okBtn.setOnClickListener {
            startAlarm()
        }
        resetBtn.setOnClickListener {
            // 'Refill' all glasses
            writeAllGlassPrefs()
            glassAmounts.fill(0)

            updateUI()

            startAlarm()
        }

        // Set onTextChanged listener for minsEditText
        minsEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.isNotEmpty()) {
                        val editor = prefs.edit()
                        editor.putInt("wait", it.toString().toInt())
                        editor.apply()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.drink_water)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("WATER_TRACKER", name, importance)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun readPrefs() {
        // Read prefs
        glassAmounts[0] = prefs.getInt("g0", 0)
        glassAmounts[1] = prefs.getInt("g1", 0)
        glassAmounts[2] = prefs.getInt("g2", 0)
        glassAmounts[3] = prefs.getInt("g3", 0)
        glassAmounts[4] = prefs.getInt("g4", 0)
        wait = prefs.getInt("wait", 0)
        running = prefs.getBoolean("running", false)

        // Update UI from read prefs
        updateUI()
    }

    private fun writeGlassAmount(glassIndex: Int) {
        val editor = prefs.edit()
        editor.putInt("g$glassIndex", glassAmounts[glassIndex])
        editor.apply()
    }

    // Sets all glass levels to 0 (full)
    private fun writeAllGlassPrefs() {
        val editor = prefs.edit()
        editor.putInt("g0", 0)
        editor.putInt("g1", 0)
        editor.putInt("g2", 0)
        editor.putInt("g3", 0)
        editor.putInt("g4", 0)
        editor.apply()
    }

    private fun putRunning(running: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean("running", running)
        editor.apply()
    }

    private fun updateUI() {
        glass1.setBackgroundResource(glassImages[glassAmounts[0]])
        glass2.setBackgroundResource(glassImages[glassAmounts[1]])
        glass3.setBackgroundResource(glassImages[glassAmounts[2]])
        glass4.setBackgroundResource(glassImages[glassAmounts[3]])
        glass5.setBackgroundResource(glassImages[glassAmounts[4]])
        minsEditText.setText("$wait")
        if (running) {
            alarmTextView.text = getString(R.string.currently_running)
        } else {
            alarmTextView.text = getString(R.string.not_running)
        }
        setLLVisibilities()
    }

    private fun glassClicked(glassIndex: Int) {
        // Animate water level decreasing (and resetting after empty)
        glassAmounts[glassIndex] = if (glassAmounts[glassIndex] == 4) {
            0
        } else {
            glassAmounts[glassIndex] + 1
        }

        // reset the alarm if all glasses aren't empty
        if (allNotEmpty()) {
            startAlarm()
        } else if (allEmpty()) {
            cancelAlarm()
        }

        // Update the UI
        val glass = when (glassIndex) {
            0 -> glass1
            1 -> glass2
            2 -> glass3
            3 -> glass4
            4 -> glass5
            else -> null
        }
        glass?.setBackgroundResource(glassImages[glassAmounts[glassIndex]])

        setLLVisibilities()

        // Update the shared preferences
        writeGlassAmount(glassIndex)
    }

    private fun setLLVisibilities() {
        if (allEmpty()) {
            congratsLinearLayout.visibility = View.VISIBLE
            timeLinearLayout.visibility = View.INVISIBLE
        } else {
            congratsLinearLayout.visibility = View.INVISIBLE
            timeLinearLayout.visibility = View.VISIBLE
        }
    }

    private fun allNotEmpty(): Boolean {
        return (glassAmounts[0] != 4 ||
                glassAmounts[1] != 4 ||
                glassAmounts[2] != 4 ||
                glassAmounts[3] != 4 ||
                glassAmounts[4] != 4)
    }

    private fun allEmpty(): Boolean {
        return (glassAmounts[0] == 4 &&
                glassAmounts[1] == 4 &&
                glassAmounts[2] == 4 &&
                glassAmounts[3] == 4 &&
                glassAmounts[4] == 4)
    }

    private fun startAlarm() {
        try {
            // Stop the alarm if there was one
            cancelAlarm()

            // Calculate wait time in millis
            val wait = minsEditText.text.toString().toInt()
            val millis = System.currentTimeMillis() + (wait * 1000 * 60)

            // Start the alarm
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
            }

            // Update UI and shared preferences
            putRunning(true)
            alarmTextView.text = getString(R.string.currently_running)
        } catch (e: NumberFormatException) {
            Toast.makeText(applicationContext, getString(R.string.no_empty_minutes), Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAlarm() {
        alarmManager.cancel(pendingIntent)
        putRunning(false)
        alarmTextView.text = getString(R.string.not_running)
    }
}