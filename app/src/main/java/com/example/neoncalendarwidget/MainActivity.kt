package com.example.neoncalendarwidget

import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var taskContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskContainer = findViewById(R.id.taskContainer)

        renderTasks()
    }

    override fun onResume() {
        super.onResume()
        renderTasks()
    }

    private fun renderTasks() {
        taskContainer.removeAllViews()
        val tasks = TodoStore.getTasks(this)

        tasks.forEachIndexed { index, task ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 14, 0, 14)
                gravity = Gravity.CENTER_VERTICAL
            }

            val checkBox = CheckBox(this).apply {
                isChecked = task.done
                text = task.title
                setTextColor(resources.getColor(R.color.text_primary, theme))
                textSize = 16f
                setOnClickListener {
                    TodoStore.toggleTask(this@MainActivity, index)
                    renderTasks()
                    TodoWidgetProvider.refreshAll(this@MainActivity)
                }
            }

            row.addView(checkBox)
            taskContainer.addView(row)
        }
    }
}
