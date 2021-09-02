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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Set running to false in preferences
        val prefs = context.getSharedPreferences("Prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("running", false)
        editor.apply()

        // PendingIntent to open MainActivity on tap
        val i = Intent(context, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent =
            PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Build and show the notification
        val builder = NotificationCompat.Builder(context, "WATER_TRACKER")
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(context.getString(R.string.drink_water))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(1, builder.build())
    }
}