package takagicom.todo.jodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 任务提醒接收器
 * 用于处理任务提醒通知
 */
class TaskReminderReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TaskReminderReceiver"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Context or intent is null")
            return
        }
        
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "未知任务"
        
        Log.d(TAG, "Received task reminder: id=$taskId, title=$taskTitle")
        
        // TODO: 在这里可以添加实际的通知逻辑
        // 例如：创建通知，显示提醒等
    }
}
