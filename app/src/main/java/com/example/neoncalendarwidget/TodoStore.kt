package com.example.neoncalendarwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TodoItem(
    val title: String,
    val done: Boolean = false
)

object TodoStore {
    private const val PREFS = "repeatit_todo_prefs"
    private const val KEY_TASKS = "tasks_json"

    fun getTasks(context: Context): List<TodoItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TASKS, "[]") ?: "[]"

        val array = JSONArray(raw)
        val items = mutableListOf<TodoItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            items.add(
                TodoItem(
                    title = obj.optString("title", "Untitled task"),
                    done = obj.optBoolean("done", false)
                )
            )
        }
        return items
    }

    fun addTask(context: Context, title: String) {
        val tasks = getTasks(context).toMutableList()
        tasks.add(TodoItem(title = title, done = false))
        save(context, tasks)
    }

    fun toggleTask(context: Context, index: Int) {
        val tasks = getTasks(context).toMutableList()
        if (index in tasks.indices) {
            val current = tasks[index]
            tasks[index] = current.copy(done = !current.done)
            save(context, tasks)
        }
    }

    private fun save(context: Context, tasks: List<TodoItem>) {
        val array = JSONArray()
        tasks.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("done", item.done)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TASKS, array.toString())
            .apply()
    }
}
