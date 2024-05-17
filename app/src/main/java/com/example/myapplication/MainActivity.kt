package com.example.myapplication

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


data class AppSession(
    val id: Int,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val screenTime: Long
)

data class AppUsageRecord(
    val id: Int,
    val appName: String,
    val date: String, // Date in yyyy-MM-dd format
    val hour: Int,    // Hour of the day (0-23)
    val screenTime: Long
)


class MainActivity : AppCompatActivity() {

    private lateinit var checkBox: CheckBox

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted
            Toast.makeText(this, "Usage stats permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied
            Toast.makeText(this, "Please grant usage stats permission to retrieve usage stats.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val buttonPermissions: Button = findViewById(R.id.button1)
        val buttonSend: Button = findViewById(R.id.button)
        checkBox = findViewById(R.id.checkBox)

        buttonPermissions.setOnClickListener {
            requestUsageStatsPermission()
        }

        buttonSend.setOnClickListener {
            if (checkBox.isChecked) {
                retrieveUsageStats()
            } else {
                Toast.makeText(this, "Please accept the checkbox to proceed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestUsageStatsPermission() {
        if (!checkUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            requestPermissionLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Usage stats permission already granted.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun retrieveUsageStats() {
        if (checkUsageStatsPermission()) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.MONTH, -1) // Start of the last month
            val startTime = calendar.timeInMillis

            // Get usage stats for each hour in the last month
            val appUsageRecords = mutableListOf<AppUsageRecord>()
            var currentTime = startTime
            while (currentTime <= endTime) {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime,
                    currentTime + TimeUnit.HOURS.toMillis(1) // End of the current hour
                )

                // Process usage stats for the current hour
                if (stats.isNotEmpty()) {
                    for (usageStats in stats) {
                        val packageName = usageStats.packageName
                        val totalUsageTime = usageStats.totalTimeInForeground

                        // Add app usage record for each app, for each hour
                        appUsageRecords.add(
                            AppUsageRecord(
                                id = appUsageRecords.size + 1, // Generate a unique ID
                                appName = packageName,
                                date = SimpleDateFormat("yyyy-MM-dd").format(currentTime), // Date in yyyy-MM-dd format
                                hour = Calendar.getInstance().apply { timeInMillis = currentTime }.get(Calendar.HOUR_OF_DAY), // Hour of the day
                                screenTime = totalUsageTime
                            )
                        )
                    }
                }

                // Move to the next hour
                currentTime += TimeUnit.HOURS.toMillis(1)
            }

            // Now you have a list of app usage records for each app, for each hour in the last month
            // You can save it to a database or display it on the screen
            displayAppUsageRecords(appUsageRecords)
        } else {
            Toast.makeText(this, "Please grant usage stats permission to retrieve usage stats.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayAppUsageRecords(appUsageRecords: List<AppUsageRecord>) {
        // Here you can display the app usage record data on the screen
        // For example, you can use a RecyclerView to display each record as a row
        // or you can simply log the data for now
        for (record in appUsageRecords) {
            val dateAndHour = "${record.date} ${record.hour}:00:00"
            val screenTimeFormatted = formatScreenTime(record.screenTime)

            // Log or display the formatted app usage record data
            val message = "Record ID: ${record.id}\n" +
                    "App Name: ${record.appName}\n" +
                    "Date and Hour: $dateAndHour\n" +
                    "Screen Time: $screenTimeFormatted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }




    private fun displayAppSessions(appSessions: List<AppSession>) {
        // Here you can display the app session data on the screen
        // For example, you can use a RecyclerView to display each app session as a row
        // or you can simply log the data for now
        for (appSession in appSessions) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val startTime = sdf.format(appSession.startTime)
            val endTime = sdf.format(appSession.endTime)
            val screenTimeFormatted = formatScreenTime(appSession.screenTime)

            // Log or display the formatted app session data
            val message = "AppSession ID: ${appSession.id}\n" +
                    "App Name: ${appSession.appName}\n" +
                    "Start Time: $startTime\n" +
                    "End Time: $endTime\n" +
                    "Screen Time: $screenTimeFormatted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatScreenTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
