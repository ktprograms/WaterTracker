/*
 * GNU General Public License v3.0
 *
 * Copyright (c) 2020 Toh Jeen Gie Keith
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
    // Declare views
    private var glass1: ImageButton? = null
    private var glass2: ImageButton? = null
    private var glass3: ImageButton? = null
    private var glass4: ImageButton? = null
    private var glass5: ImageButton? = null
    private var okBtn: Button? = null
    private var resetBtn: Button? = null
    private var timeLinearLayout: LinearLayout? = null
    private var congratsLinearLayout: LinearLayout? = null
    private var minsEditText: EditText? = null
    private var alarmTextView: TextView? = null

    // Initialize arrays
    private val glassAmounts = mutableListOf(0, 0, 0, 0, 0) // 0 -> full, 4 -> empty
    private val glassLevels = listOf(
        R.drawable.glass100,
        R.drawable.glass75,
        R.drawable.glass50,
        R.drawable.glass25,
        R.drawable.glass0
    )

    // Declare other object instances
    private var prefs: SharedPreferences? = null
    private var alarmManager: AlarmManager? = null
    private var alarmIntent: Intent? = null
    private var pendingIntent: PendingIntent? = null

    // Other variables / constants
    private var wait = 0
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        initViews()

        // Initialize other object instances
        initStuff()

        // Create a notification channel (For Android O [28] and above only)
        createNotificationChannel()

        // Read the shared preferences
        readPrefs()

        // Check if app started from the notification
        if (intent.hasExtra("com.ktprograms.watertracker.NOTIFEXTRA")) {
            putRunning(false)
            alarmTextView!!.text = "Water alarm is currently not running"
            intent.removeExtra("com.ktprograms.watertracker.NOTIFEXTRA")
        }

        // Set glass onClick listeners
        glass1!!.setOnClickListener {
            onClick(0)
        }
        glass2!!.setOnClickListener {
            onClick(1)
        }
        glass3!!.setOnClickListener {
            onClick(2)
        }
        glass4!!.setOnClickListener {
            onClick(3)
        }
        glass5!!.setOnClickListener {
            onClick(4)
        }

        // Set Ok and Reset Button onClick listeners
        okBtn!!.setOnClickListener {
            startAlarm()
        }
        resetBtn!!.setOnClickListener {
            // 'Refill' all glasses
            writeAllGlassPrefs()
            for (i in 0..4) {
                glassAmounts.set(i, 0)
            }
            updateUI()

            startAlarm()
        }

        // Set onTextChanged listener for minsEditText
        minsEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!!.isNotEmpty()) {
                    val editor = prefs!!.edit()
                    editor.putInt("wait", s.toString().toInt())
                    editor.apply()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initViews() {
        glass1 = findViewById(R.id.glass1)
        glass2 = findViewById(R.id.glass2)
        glass3 = findViewById(R.id.glass3)
        glass4 = findViewById(R.id.glass4)
        glass5 = findViewById(R.id.glass5)

        okBtn = findViewById(R.id.okBtn)
        resetBtn = findViewById(R.id.resetBtn)

        timeLinearLayout = findViewById(R.id.timeLinearLayout)
        congratsLinearLayout = findViewById(R.id.congratsLinearLayout)

        minsEditText = findViewById(R.id.minsEditText)

        alarmTextView = findViewById(R.id.alarmTextView)
    }

    private fun initStuff() {
        prefs = getPreferences(MODE_PRIVATE)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(applicationContext, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, alarmIntent, 0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Drink water!!!"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("WATER_TRACKER", name, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun readPrefs() {
        // Read prefs
        glassAmounts[0] = prefs!!.getInt("g1", 0)
        glassAmounts[1] = prefs!!.getInt("g2", 0)
        glassAmounts[2] = prefs!!.getInt("g3", 0)
        glassAmounts[3] = prefs!!.getInt("g4", 0)
        glassAmounts[4] = prefs!!.getInt("g5", 0)
        wait = prefs!!.getInt("wait", 0)
        running = prefs!!.getBoolean("running", false)

        // Update UI from read prefs
        updateUI()
    }

    // i is the glassAmount index
    private fun writePrefs(i: Int) {
        val i1 = i + 1
        val editor = prefs!!.edit()
        editor.putInt("g$i1", glassAmounts[i])
        editor.apply()
    }

    // Sets all glass levels to 0 (full)
    private fun writeAllGlassPrefs() {
        val editor = prefs!!.edit()
        editor.putInt("g1", 0)
        editor.putInt("g2", 0)
        editor.putInt("g3", 0)
        editor.putInt("g4", 0)
        editor.putInt("g5", 0)
        editor.apply()
    }

    private fun putRunning(b: Boolean) {
        val editor = prefs!!.edit()
        editor.putBoolean("running", b)
        editor.apply()
    }

    private fun updateUI() {
        glass1!!.setBackgroundResource(glassLevels[glassAmounts[0]])
        glass2!!.setBackgroundResource(glassLevels[glassAmounts[1]])
        glass3!!.setBackgroundResource(glassLevels[glassAmounts[2]])
        glass4!!.setBackgroundResource(glassLevels[glassAmounts[3]])
        glass5!!.setBackgroundResource(glassLevels[glassAmounts[4]])
        minsEditText!!.setText("$wait")
        val displayText = if (running) "" else " not"
        alarmTextView!!.text = "Water alarm is currently$displayText running"
        setLLVisibilities()
    }

    // i is the glassAmount index
    private fun onClick(i: Int) {
        // Animate water level decreasing (and resetting after empty)
        glassAmounts[i] = if (glassAmounts[i] == 4) {
            0
        } else {
            glassAmounts[i] + 1
        }

        // reset the alarm if all glasses aren't empty
        if (allNotEmpty()) {
            startAlarm()
        }

        // Update the UI
        val glass = when (i) {
            0 -> glass1
            1 -> glass2
            2 -> glass3
            3 -> glass4
            4 -> glass5
            else -> null
        }
        glass!!.setBackgroundResource(glassLevels[glassAmounts[i]])
        setLLVisibilities()

        // Update the shared preferences
        writePrefs(i)
    }

    private fun setLLVisibilities() {
        if (allEmpty()) {
            congratsLinearLayout!!.visibility = View.VISIBLE
            timeLinearLayout!!.visibility = View.INVISIBLE
            cancelAlarm()
        } else {
            congratsLinearLayout!!.visibility = View.INVISIBLE
            timeLinearLayout!!.visibility = View.VISIBLE
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
        val wait = minsEditText!!.text.toString().toInt()
        val millis = System.currentTimeMillis() + (wait * 1000 * 60)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            alarmManager!!.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } else {
            alarmManager!!.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        }
        putRunning(true)
        alarmTextView!!.text = "Water alarm is currently running"
    }

    private fun cancelAlarm() {
        alarmManager!!.cancel(pendingIntent)
        putRunning(false)
        alarmTextView!!.text = "Water alarm is currently not running"
    }
}