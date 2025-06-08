package takagicom.todo.jodo.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.repository.TaskRepository
import java.time.LocalDateTime

class QuickAddTaskMinimalActivity : Activity() {
      private lateinit var taskRepository: TaskRepository
    private lateinit var taskInput: EditText
    private lateinit var confirmButton: MaterialButton
    private var buttonTimeoutHandler: Handler? = null
    private var buttonTimeoutRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("QuickAddTask", "QuickAddTaskMinimalActivity onCreate called")
        
        // 设置为透明背景
        setContentView(R.layout.activity_quick_add_task_minimal)
        
        // 设置窗口属性以在输入法上方显示
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
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
        
        // 自动弹出输入法
        showKeyboard()
    }
    
    private fun initViews() {
        taskInput = findViewById(R.id.task_input)
        confirmButton = findViewById(R.id.confirm_button)
        
        // 自动聚焦到输入框
        taskInput.requestFocus()
    }    private fun setupListeners() {
        // 确认按钮点击事件
        confirmButton.setOnClickListener {
            android.util.Log.d("QuickAddTask", "Confirm button clicked")
            
            // 检查按钮是否已经被禁用，防止重复点击
            if (!confirmButton.isEnabled) {
                android.util.Log.d("QuickAddTask", "Button already disabled, ignoring click")
                return@setOnClickListener
            }
            
            // 提供即时视觉反馈
            confirmButton.isEnabled = false
            
            // 设置超时机制，防止按钮永久禁用
            setupButtonTimeout()
            
            addTask()
        }
        
        // 输入框回车键事件
        taskInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                android.util.Log.d("QuickAddTask", "Enter key pressed")
                addTask()
                true
            } else {
                false
            }
        }
          // 点击外部区域关闭
        findViewById<View>(R.id.background_overlay).setOnClickListener {
            android.util.Log.d("QuickAddTask", "Background overlay clicked")
            hideKeyboardAndFinish()
        }
    }
    
    private fun setupButtonTimeout() {
        // 清除之前的超时
        buttonTimeoutRunnable?.let { buttonTimeoutHandler?.removeCallbacks(it) }
        
        // 设置新的超时机制（5秒后重新启用按钮）
        buttonTimeoutHandler = Handler(Looper.getMainLooper())
        buttonTimeoutRunnable = Runnable {
            android.util.Log.w("QuickAddTask", "Button timeout reached, re-enabling button")
            confirmButton.isEnabled = true
        }
        buttonTimeoutHandler?.postDelayed(buttonTimeoutRunnable!!, 5000)
    }
    
    private fun cancelButtonTimeout() {
        buttonTimeoutRunnable?.let { 
            buttonTimeoutHandler?.removeCallbacks(it)
            buttonTimeoutRunnable = null
        }
    }private fun addTask() {
        val description = taskInput.text.toString().trim()
        android.util.Log.d("QuickAddTask", "addTask called with description: '$description'")
        
        if (description.isNotEmpty()) {
            android.util.Log.d("QuickAddTask", "Starting task creation in background thread")
            
            // 在IO线程中执行所有数据库操作
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    android.util.Log.d("QuickAddTask", "Getting next ID from repository")
                    val nextId = taskRepository.getNextId()
                    android.util.Log.d("QuickAddTask", "Next ID: $nextId")
                    
                    val newTask = Task(
                        id = nextId,
                        description = description,
                        completed = false,
                        starred = false,
                        created_at = LocalDateTime.now(),
                        due_date = null,
                        categoryId = 0L,
                        deleted = false
                    )
                    
                    android.util.Log.d("QuickAddTask", "Adding task to repository")
                    taskRepository.addTask(newTask)
                    android.util.Log.d("QuickAddTask", "Task added successfully: $description")                    // 在主线程中通知小组件刷新
                    launch(Dispatchers.Main) {
                        android.util.Log.d("QuickAddTask", "Sending broadcast and finishing activity")
                        
                        // 取消超时并重新启用按钮（虽然马上会关闭）
                        cancelButtonTimeout()
                        
                        // 发送任务数据变更广播
                        val intent = Intent("TASK_DATA_CHANGED")
                        sendBroadcast(intent)
                        
                        // 更新小组件 - 使用正确的方式
                        val appWidgetManager = AppWidgetManager.getInstance(this@QuickAddTaskMinimalActivity)
                        val componentName = android.content.ComponentName(this@QuickAddTaskMinimalActivity, SimpleTaskWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        
                        if (appWidgetIds.isNotEmpty()) {
                            android.util.Log.d("QuickAddTask", "Updating ${appWidgetIds.size} widgets")
                            // 直接调用小组件的onUpdate方法
                            val widgetProvider = SimpleTaskWidgetProvider()
                            widgetProvider.onUpdate(this@QuickAddTaskMinimalActivity, appWidgetManager, appWidgetIds)
                            
                            // 通知数据变更
                            for (appWidgetId in appWidgetIds) {
                                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
                            }
                        } else {
                            android.util.Log.w("QuickAddTask", "No widgets found to update")
                        }
                        
                        hideKeyboardAndFinish()
                    }} catch (e: Exception) {
                    android.util.Log.e("QuickAddTask", "Error adding task", e)
                    launch(Dispatchers.Main) {
                        // 取消超时并重新启用按钮
                        cancelButtonTimeout()
                        confirmButton.isEnabled = true
                        android.util.Log.e("QuickAddTask", "Re-enabled button due to error")
                        
                        // 可以选择显示错误提示而不是直接关闭
                        hideKeyboardAndFinish()
                    }
                }
            }        } else {
            android.util.Log.d("QuickAddTask", "Empty description, re-enabling button")
            cancelButtonTimeout()
            confirmButton.isEnabled = true
            // 空描述时不关闭界面，让用户重新输入
        }
    }
    
    private fun showKeyboard() {
        taskInput.post {
            taskInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(taskInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    private fun hideKeyboardAndFinish() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(taskInput.windowToken, 0)
        finish()
    }
    
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        // 点击外部区域关闭活动
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            hideKeyboardAndFinish()
        }
        return super.onTouchEvent(event)
    }
      override fun onBackPressed() {
        hideKeyboardAndFinish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        cancelButtonTimeout()
    }
}
