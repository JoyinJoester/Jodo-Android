package takagicom.todo.jodo.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.repository.TaskRepository
import java.time.LocalDateTime

class QuickAddTaskActivity : Activity() {
    
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskInput: EditText
    private lateinit var addButton: Button
    private lateinit var cancelButton: ImageButton
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("QuickAddTask", "QuickAddTaskActivity onCreate called")
        
        // 设置为透明背景的对话框样式
        setContentView(R.layout.activity_quick_add_task)
        
        // 设置窗口属性
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        
        taskRepository = TaskRepository(this)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        taskInput = findViewById(R.id.task_input)
        addButton = findViewById(R.id.add_button)
        cancelButton = findViewById(R.id.cancel_button)
        
        // 自动聚焦到输入框
        taskInput.requestFocus()
    }
    
    private fun setupListeners() {
        addButton.setOnClickListener {
            addTask()
        }
        
        cancelButton.setOnClickListener {
            finish()
        }
        
        // 点击外部区域关闭
        findViewById<View>(R.id.background_overlay).setOnClickListener {
            finish()
        }
    }
      private fun addTask() {
        val description = taskInput.text.toString().trim()
        if (description.isNotEmpty()) {            val newTask = Task(
                id = taskRepository.getNextId(),
                description = description,
                completed = false,
                starred = false,
                created_at = LocalDateTime.now(),
                due_date = null,
                categoryId = 0L,
                deleted = false
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    taskRepository.addTask(newTask)
                    android.util.Log.d("QuickAddTask", "Task added successfully: $description")
                    
                    // 在主线程中通知小组件刷新
                    launch(Dispatchers.Main) {
                        val intent = Intent("TASK_DATA_CHANGED")
                        sendBroadcast(intent)
                        
                        // 也发送小组件更新广播
                        val updateIntent = Intent(this@QuickAddTaskActivity, SimpleTaskWidgetProvider::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        }
                        sendBroadcast(updateIntent)
                        
                        finish()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuickAddTask", "Error adding task", e)
                    launch(Dispatchers.Main) {
                        finish()
                    }
                }
            }
        }
    }
    
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        // 点击外部区域关闭活动
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            finish()
        }
        return super.onTouchEvent(event)
    }
}
