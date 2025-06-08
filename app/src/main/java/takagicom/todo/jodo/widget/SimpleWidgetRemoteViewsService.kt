package takagicom.todo.jodo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.util.Log
import kotlinx.coroutines.*
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.repository.TaskRepository

class SimpleWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SimpleWidgetRemoteViewsFactory(this.applicationContext, intent)
    }
}

class SimpleWidgetRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    
    private var tasks: List<Task> = emptyList()
      override fun onCreate() {
        Log.d("SimpleWidgetFactory", "onCreate called")
        // 在onCreate中强制同步加载数据
        loadTasksSync()
    }

    override fun onDataSetChanged() {
        Log.d("SimpleWidgetFactory", "onDataSetChanged called")
        // 在onDataSetChanged中强制同步加载数据
        loadTasksSync()
    }      private fun loadTasksSync() {
        try {
            // 直接从本地文件同步加载任务数据
            val file = java.io.File(context.filesDir, "tasks.json")
            if (file.exists()) {
                val json = file.readText()
                val gson = createTaskGson()
                val typeToken = object : com.google.gson.reflect.TypeToken<List<Task>>() {}.type
                val allTasks = gson.fromJson<List<Task>>(json, typeToken) ?: emptyList()
                  // 获取筛选设置
                val prefs = context.getSharedPreferences("widget_preferences", android.content.Context.MODE_PRIVATE)
                val filterType = prefs.getString("filter_type", "all")
                val selectedCategoryId = prefs.getLong("selected_category_id", 0L)
                
                // 应用筛选器
                val filteredTasks = when (filterType) {
                    "all" -> allTasks.filter { !it.deleted }
                    "in_progress" -> allTasks.filter { !it.deleted && !it.completed }
                    "completed" -> allTasks.filter { !it.deleted && it.completed }
                    "starred" -> allTasks.filter { !it.deleted && it.starred }
                    "category" -> {
                        if (selectedCategoryId != 0L) {
                            allTasks.filter { !it.deleted && it.categoryId == selectedCategoryId }
                        } else {
                            allTasks.filter { !it.deleted }
                        }
                    }
                    else -> allTasks.filter { !it.deleted }
                }
                  // 显示过滤后的任务，排序规则与主应用保持一致
                tasks = filteredTasks.sortedWith(compareBy<Task> { it.completed }  // 未完成的任务在前
                        .thenByDescending { it.starred }           // 星标任务在前
                        .thenByDescending { it.created_at }        // 新创建的任务在前
                        .thenBy { it.due_date }                    // 按截止日期排序
                        .thenBy { it.description })                // 最后按描述排序
                
                Log.d("SimpleWidgetFactory", "Loaded ${tasks.size} tasks from file (filterType: $filterType, categoryId: $selectedCategoryId)")
                Log.d("SimpleWidgetFactory", "Total tasks before filter: ${allTasks.filter { !it.deleted }.size}")
                tasks.forEachIndexed { index, task ->
                    Log.d("SimpleWidgetFactory", "Filtered Task $index: ${task.description} - completed:${task.completed}, starred:${task.starred}, categoryId:${task.categoryId}")
                }
            } else {
                Log.d("SimpleWidgetFactory", "Tasks file does not exist")
                tasks = emptyList()
            }
        } catch (e: Exception) {
            Log.e("SimpleWidgetFactory", "Error loading tasks", e)
            tasks = emptyList()
        }
    }
    
    private fun createTaskGson(): com.google.gson.Gson {
        return com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(Task::class.java, TaskDeserializer())
            .registerTypeAdapter(takagicom.todo.jodo.model.RepeatSettings::class.java, RepeatSettingsAdapter())
            .create()
    }

    override fun onDestroy() {
        Log.d("SimpleWidgetFactory", "onDestroy called")
        tasks = emptyList()
    }    override fun getCount(): Int {
        val count = tasks.size
        Log.d("SimpleWidgetFactory", "getCount returning: $count")
        return count
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position >= tasks.size) {
            Log.w("SimpleWidgetFactory", "getViewAt: position $position >= tasks.size ${tasks.size}")
            return null
        }

        return try {
            val task = tasks[position]
            val views = RemoteViews(context.packageName, R.layout.widget_task_item_simple)            // 设置任务标题
            views.setTextViewText(R.id.task_title, task.description)

            // 设置完成状态
            val checkboxIcon = if (task.completed) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
            views.setImageViewResource(R.id.task_checkbox, checkboxIcon)
            
            // 设置收藏状态
            val starIcon = if (task.starred) R.drawable.ic_star else R.drawable.ic_star_border
            views.setImageViewResource(R.id.task_favorite, starIcon)
            
            // 设置截止日期
            if (task.due_date != null) {
                val dueDateText = formatDueDate(task.due_date)
                views.setTextViewText(R.id.task_due_date, dueDateText)
                views.setViewVisibility(R.id.task_due_date, android.view.View.VISIBLE)
                
                // 如果过期了，设置红色文字
                if (task.due_date.isBefore(java.time.LocalDateTime.now())) {
                    views.setTextColor(R.id.task_due_date, context.getColor(android.R.color.holo_red_light))
                } else {
                    views.setTextColor(R.id.task_due_date, 0xCCFFFFFF.toInt())
                }
            } else {
                views.setViewVisibility(R.id.task_due_date, android.view.View.GONE)
            }            // 设置点击事件
            android.util.Log.d("SimpleWidgetFactory", "Setting click events for task: ${task.description}")

            // 为checkbox设置完成事件
            val completeIntent = Intent().apply {
                action = SimpleTaskWidgetProvider.ACTION_TASK_COMPLETE
                putExtra(SimpleTaskWidgetProvider.EXTRA_TASK_ID, task.id.toString())
            }
            views.setOnClickFillInIntent(R.id.task_checkbox, completeIntent)
            android.util.Log.d("SimpleWidgetFactory", "Set complete intent for task ${task.id}")

            // 为星标设置收藏事件
            val favoriteIntent = Intent().apply {
                action = SimpleTaskWidgetProvider.ACTION_TASK_FAVORITE
                putExtra(SimpleTaskWidgetProvider.EXTRA_TASK_ID, task.id.toString())
            }
            views.setOnClickFillInIntent(R.id.task_favorite, favoriteIntent)
            android.util.Log.d("SimpleWidgetFactory", "Set favorite intent for task ${task.id}")

            // 为整个item设置默认点击事件（打开应用）
            val titleClickIntent = Intent().apply {
                action = "TASK_ITEM_CLICK"
                putExtra(SimpleTaskWidgetProvider.EXTRA_TASK_ID, task.id.toString())
            }
            views.setOnClickFillInIntent(R.id.task_title, titleClickIntent)
            android.util.Log.d("SimpleWidgetFactory", "Set title click intent for task ${task.id}")

            Log.d("SimpleWidgetFactory", "Created view for task: ${task.description}")
            views
        } catch (e: Exception) {
            Log.e("SimpleWidgetFactory", "Error creating view at position $position", e)
            createErrorView()
        }
    }    private fun createErrorView(): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item_simple)
        views.setTextViewText(R.id.task_title, "加载错误")
        views.setViewVisibility(R.id.task_due_date, android.view.View.GONE)
        views.setImageViewResource(R.id.task_checkbox, R.drawable.ic_checkbox_unchecked)
        views.setImageViewResource(R.id.task_favorite, R.drawable.ic_star_border)
        return views
    }
    
    private fun formatDueDate(dueDate: java.time.LocalDateTime): String {
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val dueDateLocal = dueDate.toLocalDate()
        
        return when {
            dueDate.isBefore(now) -> "已过期"
            dueDateLocal.isEqual(today) -> "今天"
            dueDateLocal.isEqual(tomorrow) -> "明天"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
                dueDate.format(formatter)
            }        }
    }
      override fun getLoadingView(): RemoteViews? {
        Log.d("SimpleWidgetFactory", "getLoadingView called")
        // 返回null以避免显示加载视图，让小组件直接显示实际内容
        return null
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    
    override fun hasStableIds(): Boolean = true
    
    // JSON适配器类
    class LocalDateTimeAdapter : com.google.gson.JsonSerializer<java.time.LocalDateTime>, com.google.gson.JsonDeserializer<java.time.LocalDateTime> {
        private val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: java.time.LocalDateTime?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
            return com.google.gson.JsonPrimitive(src?.format(formatter) ?: "")
        }
          
        override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): java.time.LocalDateTime? {
            val dateString = json?.asString
            return if (dateString.isNullOrEmpty()) {
                null
            } else {
                try {
                    java.time.LocalDateTime.parse(dateString, formatter)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    class TaskDeserializer : com.google.gson.JsonDeserializer<Task> {
        override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): Task? {
            if (json == null || !json.isJsonObject) return null
            
            val jsonObject = json.asJsonObject
            
            return try {
                Task(
                    id = jsonObject.get("id")?.asLong ?: 0L,
                    description = jsonObject.get("description")?.asString ?: "",
                    completed = jsonObject.get("completed")?.asBoolean ?: false,
                    created_at = context?.deserialize(jsonObject.get("created_at"), java.time.LocalDateTime::class.java) ?: java.time.LocalDateTime.now(),
                    due_date = context?.deserialize(jsonObject.get("due_date"), java.time.LocalDateTime::class.java),
                    starred = jsonObject.get("starred")?.asBoolean ?: false,
                    deleted = jsonObject.get("deleted")?.asBoolean ?: false,
                    reminder = jsonObject.get("reminder")?.asBoolean ?: false,
                    reminder_time = context?.deserialize(jsonObject.get("reminder_time"), java.time.LocalDateTime::class.java),
                    repeat = try {
                        takagicom.todo.jodo.model.RepeatInterval.valueOf(jsonObject.get("repeat")?.asString ?: "NONE")
                    } catch (e: Exception) {
                        takagicom.todo.jodo.model.RepeatInterval.NONE
                    },
                    repeatSettings = context?.deserialize(jsonObject.get("repeatSettings"), takagicom.todo.jodo.model.RepeatSettings::class.java),
                    categoryId = jsonObject.get("categoryId")?.asLong ?: 0L
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    class RepeatSettingsAdapter : com.google.gson.JsonSerializer<takagicom.todo.jodo.model.RepeatSettings>, com.google.gson.JsonDeserializer<takagicom.todo.jodo.model.RepeatSettings> {
        override fun serialize(src: takagicom.todo.jodo.model.RepeatSettings?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
            if (src == null) return com.google.gson.JsonPrimitive("")
            
            val gson = com.google.gson.Gson()
            return gson.toJsonTree(src)
        }
        
        override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): takagicom.todo.jodo.model.RepeatSettings? {
            if (json == null || json.isJsonNull || (json.isJsonPrimitive && json.asString.isEmpty())) {
                return null
            }
            
            return try {
                val gson = com.google.gson.GsonBuilder()
                    .registerTypeAdapter(java.time.LocalDateTime::class.java, LocalDateTimeAdapter())
                    .create()
                gson.fromJson(json, takagicom.todo.jodo.model.RepeatSettings::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
