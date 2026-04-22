package com.example.neoncalendarwidget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.neoncalendarwidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            binding.statusText.text = if (granted) {
                "Calendar permission granted. Add the widget from your home screen."
            } else {
                "Calendar permission denied. The widget will show no events."
            }
            refreshWidget()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.requestPermissionButton.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        binding.refreshButton.setOnClickListener {
            refreshWidget()
            binding.statusText.text = "Widget refreshed."
        }

        binding.statusText.text = "Grant calendar permission, then add the widget from the widget picker."
    }

    override fun onResume() {
        super.onResume()
        refreshWidget()
    }

    private fun refreshWidget() {
        val intent = Intent(this, CalendarWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                AppWidgetManager.getInstance(this@MainActivity)
                    .getAppWidgetIds(ComponentName(this@MainActivity, CalendarWidgetProvider::class.java))
            )
        }
        sendBroadcast(intent)
    }
}
