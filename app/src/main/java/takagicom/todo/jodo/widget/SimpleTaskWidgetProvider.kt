package takagicom.todo.jodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import takagicom.todo.jodo.MainActivity
import takagicom.todo.jodo.R
import takagicom.todo.jodo.repository.CategoryRepository
import takagicom.todo.jodo.repository.TaskRepository

class SimpleTaskWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("SimpleTaskWidget", "onUpdate called with ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        
        // 强制刷新所有小组件数据
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
        }
    }
      override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("SimpleTaskWidget", "=== onReceive DEBUG START ===")
        android.util.Log.d("SimpleTaskWidget", "onReceive called with action: ${intent.action}")
        android.util.Log.d("SimpleTaskWidget", "Intent package: ${intent.`package`}")
        android.util.Log.d("SimpleTaskWidget", "Intent data: ${intent.data}")
        android.util.Log.d("SimpleTaskWidget", "Intent extras count: ${intent.extras?.size() ?: 0}")
        logIntentDetails(intent)
        android.util.Log.d("SimpleTaskWidget", "=== onReceive DEBUG END ===")
        
        when (intent.action) {
            ACTION_CATEGORY_CLICK -> {
                android.util.Log.d("SimpleTaskWidget", "Handling category click")
                // 处理分类选择点击
                handleCategoryClick(context)
            }
            ACTION_ADD_TASK_CLICK -> {
                android.util.Log.d("SimpleTaskWidget", "Handling add task click")
                // 处理添加任务点击
                handleAddTaskClick(context)
            }
            ACTION_TASK_COMPLETE -> {
                // 处理任务完成点击
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                android.util.Log.d("SimpleTaskWidget", "Handling task complete click for ID: $taskId")
                handleTaskComplete(context, taskId)
            }
            ACTION_TASK_FAVORITE -> {
                // 处理收藏点击
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                android.util.Log.d("SimpleTaskWidget", "Handling task favorite click for ID: $taskId")
                handleTaskFavorite(context, taskId)
            }
            "TASK_DATA_CHANGED" -> {
                // 处理数据变更广播
                android.util.Log.d("SimpleTaskWidget", "Received TASK_DATA_CHANGED broadcast")
                refreshWidget(context)
            }
            "TASK_ITEM_CLICK" -> {
                // 处理任务项点击的默认动作
                android.util.Log.d("SimpleTaskWidget", "Handling task item click")
                // 这里可以处理默认的任务项点击行为，比如打开主应用
            }
            else -> {
                android.util.Log.d("SimpleTaskWidget", "Unhandled action: ${intent.action}")
            }
        }
    }private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        android.util.Log.d("SimpleTaskWidget", "updateAppWidget called for widget $appWidgetId")
        val views = RemoteViews(context.packageName, R.layout.widget_simple)
        
        // 更新分类按钮显示文本
        updateCategoryButtonText(context, views)
          // 设置分类按钮点击事件
        val categoryIntent = Intent(context, SimpleTaskWidgetProvider::class.java).apply {
            action = ACTION_CATEGORY_CLICK
        }
        val categoryPendingIntent = PendingIntent.getBroadcast(
            context, 0, categoryIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_category_button, categoryPendingIntent)
        android.util.Log.d("SimpleTaskWidget", "Set category button click event")
        
        // 设置添加任务按钮点击事件
        val addTaskIntent = Intent(context, SimpleTaskWidgetProvider::class.java).apply {
            action = ACTION_ADD_TASK_CLICK
        }
        val addTaskPendingIntent = PendingIntent.getBroadcast(
            context, 1, addTaskIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_task_button, addTaskPendingIntent)
        android.util.Log.d("SimpleTaskWidget", "Set add task button click event")
        
        // 设置任务列表
        val serviceIntent = Intent(context, SimpleWidgetRemoteViewsService::class.java)
        views.setRemoteAdapter(R.id.widget_task_list, serviceIntent)
        android.util.Log.d("SimpleTaskWidget", "Set remote adapter for widget $appWidgetId")        // 设置任务列表项点击模板 - 这是关键！
        val taskClickTemplateIntent = Intent(context, SimpleTaskWidgetProvider::class.java)
        val taskClickTemplatePendingIntent = PendingIntent.getBroadcast(
            context, 2, taskClickTemplateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_task_list, taskClickTemplatePendingIntent)
        android.util.Log.d("SimpleTaskWidget", "Set pending intent template for list")
        
        // 设置空视图
        views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
        android.util.Log.d("SimpleTaskWidget", "Widget $appWidgetId updated and notified")
    }

    private fun handleCategoryClick(context: Context) {
        // 启动透明的分类选择Activity
        val intent = Intent(context, CategorySelectorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
      private fun handleAddTaskClick(context: Context) {
        android.util.Log.d("SimpleTaskWidget", "handleAddTaskClick called")
        try {
            // 打开简洁的快速添加任务Activity
            val intent = Intent(context, QuickAddTaskMinimalActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            android.util.Log.d("SimpleTaskWidget", "QuickAddTaskMinimalActivity started successfully")
        } catch (e: Exception) {
            android.util.Log.e("SimpleTaskWidget", "Error starting QuickAddTaskMinimalActivity", e)
        }
    }
      private fun handleTaskComplete(context: Context, taskId: String?) {
        taskId?.let { idString ->
            try {
                val id = idString.toLongOrNull() ?: return
                android.util.Log.d("SimpleTaskWidget", "Handling task complete for ID: $id")
                
                val taskRepository = TaskRepository(context)
                
                // 强制同步加载任务数据
                GlobalScope.launch {
                    try {
                        // 直接从文件读取任务
                        val file = java.io.File(context.filesDir, "tasks.json")
                        if (!file.exists()) {
                            android.util.Log.w("SimpleTaskWidget", "Tasks file does not exist")
                            return@launch
                        }
                        
                        val json = file.readText()
                        val gson = com.google.gson.GsonBuilder()
                            .registerTypeAdapter(java.time.LocalDateTime::class.java, 
                                object : com.google.gson.JsonDeserializer<java.time.LocalDateTime> {
                                    override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): java.time.LocalDateTime {
                                        return java.time.LocalDateTime.parse(json.asString)
                                    }
                                })
                            .registerTypeAdapter(java.time.LocalDateTime::class.java,
                                object : com.google.gson.JsonSerializer<java.time.LocalDateTime> {
                                    override fun serialize(src: java.time.LocalDateTime, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
                                        return com.google.gson.JsonPrimitive(src.toString())
                                    }
                                })
                            .create()
                        
                        val typeToken = object : com.google.gson.reflect.TypeToken<List<takagicom.todo.jodo.model.Task>>() {}.type
                        val tasks = gson.fromJson<List<takagicom.todo.jodo.model.Task>>(json, typeToken) ?: emptyList()
                        
                        android.util.Log.d("SimpleTaskWidget", "Loaded ${tasks.size} tasks from file")
                        val task = tasks.find { it.id == id }
                        
                        if (task != null) {
                            android.util.Log.d("SimpleTaskWidget", "Found task: ${task.description}, current completed: ${task.completed}")
                            // 切换任务完成状态
                            val updatedTask = task.copy(completed = !task.completed)
                            
                            // 更新任务列表
                            val updatedTasks = tasks.map { if (it.id == id) updatedTask else it }
                            
                            // 保存到文件
                            val updatedJson = gson.toJson(updatedTasks)
                            file.writeText(updatedJson)
                            
                            android.util.Log.d("SimpleTaskWidget", "Task updated, new completed: ${updatedTask.completed}")
                            
                            // 发送广播通知数据更改
                            val intent = Intent("TASK_DATA_CHANGED")
                            context.sendBroadcast(intent)
                            
                            // 在主线程刷新小组件
                            launch(Dispatchers.Main) {
                                refreshWidget(context)
                            }
                        } else {
                            android.util.Log.w("SimpleTaskWidget", "Task not found with ID: $id in ${tasks.size} tasks")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SimpleTaskWidget", "Error in file-based task update", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleTaskWidget", "Error updating task completion", e)
            }
        } ?: android.util.Log.w("SimpleTaskWidget", "Task ID is null")
    }
      private fun handleTaskFavorite(context: Context, taskId: String?) {
        taskId?.let { idString ->
            try {
                val id = idString.toLongOrNull() ?: return
                android.util.Log.d("SimpleTaskWidget", "Handling task favorite for ID: $id")
                
                // 强制同步加载任务数据
                GlobalScope.launch {
                    try {
                        // 直接从文件读取任务
                        val file = java.io.File(context.filesDir, "tasks.json")
                        if (!file.exists()) {
                            android.util.Log.w("SimpleTaskWidget", "Tasks file does not exist")
                            return@launch
                        }
                        
                        val json = file.readText()
                        val gson = com.google.gson.GsonBuilder()
                            .registerTypeAdapter(java.time.LocalDateTime::class.java, 
                                object : com.google.gson.JsonDeserializer<java.time.LocalDateTime> {
                                    override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): java.time.LocalDateTime {
                                        return java.time.LocalDateTime.parse(json.asString)
                                    }
                                })
                            .registerTypeAdapter(java.time.LocalDateTime::class.java,
                                object : com.google.gson.JsonSerializer<java.time.LocalDateTime> {
                                    override fun serialize(src: java.time.LocalDateTime, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
                                        return com.google.gson.JsonPrimitive(src.toString())
                                    }
                                })
                            .create()
                        
                        val typeToken = object : com.google.gson.reflect.TypeToken<List<takagicom.todo.jodo.model.Task>>() {}.type
                        val tasks = gson.fromJson<List<takagicom.todo.jodo.model.Task>>(json, typeToken) ?: emptyList()
                        
                        android.util.Log.d("SimpleTaskWidget", "Loaded ${tasks.size} tasks from file")
                        val task = tasks.find { it.id == id }
                        
                        if (task != null) {
                            android.util.Log.d("SimpleTaskWidget", "Found task: ${task.description}, current starred: ${task.starred}")
                            // 切换任务收藏状态
                            val updatedTask = task.copy(starred = !task.starred)
                            
                            // 更新任务列表
                            val updatedTasks = tasks.map { if (it.id == id) updatedTask else it }
                            
                            // 保存到文件
                            val updatedJson = gson.toJson(updatedTasks)
                            file.writeText(updatedJson)
                            
                            android.util.Log.d("SimpleTaskWidget", "Task updated, new starred: ${updatedTask.starred}")
                            
                            // 发送广播通知数据更改
                            val intent = Intent("TASK_DATA_CHANGED")
                            context.sendBroadcast(intent)
                            
                            // 在主线程刷新小组件
                            launch(Dispatchers.Main) {
                                refreshWidget(context)
                            }
                        } else {
                            android.util.Log.w("SimpleTaskWidget", "Task not found with ID: $id in ${tasks.size} tasks")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SimpleTaskWidget", "Error in file-based task favorite update", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleTaskWidget", "Error updating task favorite", e)
            }
        } ?: android.util.Log.w("SimpleTaskWidget", "Task ID is null")
    }
    
    private fun refreshWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = this.javaClass
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, thisWidget)
        )
        onUpdate(context, appWidgetManager, appWidgetIds)
    }    private fun logIntentDetails(intent: Intent) {
        android.util.Log.d("SimpleTaskWidget", "Intent action: ${intent.action}")
        android.util.Log.d("SimpleTaskWidget", "Intent extras:")
        intent.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                android.util.Log.d("SimpleTaskWidget", "  $key: ${bundle.get(key)}")
            }
        }
    }      private fun updateCategoryButtonText(context: Context, views: RemoteViews) {
        val sharedPreferences = context.getSharedPreferences("widget_preferences", Context.MODE_PRIVATE)
        val filterType = sharedPreferences.getString("filter_type", "all")
        val categoryId = sharedPreferences.getLong("selected_category_id", -1L)
        
        val categoryText = when (filterType) {
            "all" -> "全部"
            "in_progress" -> "进行中"
            "completed" -> "已完成"
            "starred" -> "收藏"
            "category" -> {
                if (categoryId != -1L) {
                    // 动态获取分类名称
                    getCategoryNameById(context, categoryId)
                } else {
                    "全部"
                }
            }
            else -> "全部"
        }
        
        views.setTextViewText(R.id.widget_category_button, categoryText)
        android.util.Log.d("SimpleTaskWidget", "Updated category button text to: $categoryText (filter_type: $filterType)")
    }
    
    private fun getCategoryNameById(context: Context, categoryId: Long): String {
        return try {
            val categoryRepository = CategoryRepository(context)
            val categories = categoryRepository.categories.value
            val category = categories.find { it.id == categoryId }
            category?.name ?: "未知分类"
        } catch (e: Exception) {
            android.util.Log.e("SimpleTaskWidget", "Error getting category name", e)
            "分类$categoryId"
        }
    }

    companion object {
        const val ACTION_CATEGORY_CLICK = "takagicom.todo.jodo.widget.CATEGORY_CLICK"
        const val ACTION_ADD_TASK_CLICK = "takagicom.todo.jodo.widget.ADD_TASK_CLICK"
        const val ACTION_TASK_COMPLETE = "takagicom.todo.jodo.widget.TASK_COMPLETE"
        const val ACTION_TASK_FAVORITE = "takagicom.todo.jodo.widget.TASK_FAVORITE"
        const val EXTRA_TASK_ID = "task_id"
    }
}
