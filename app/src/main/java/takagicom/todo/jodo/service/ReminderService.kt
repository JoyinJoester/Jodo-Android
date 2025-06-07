package takagicom.todo.jodo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import takagicom.todo.jodo.MainActivity
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.repository.TaskRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L) // 修复：使用-1L而不是-1
        if (taskId == -1L) return Result.failure()

        val repository = TaskRepository(applicationContext)
        val task = repository.getTaskById(taskId) ?: return Result.failure()
          // 显示通知
        showTaskReminderNotification(task)
        
        // 如果任务设置了重复，则安排下一次提醒
        if (task.repeat != takagicom.todo.jodo.model.RepeatInterval.NONE) {
            // 使用CoroutineScope(Dispatchers.IO)替代GlobalScope
            CoroutineScope(Dispatchers.IO).launch {
                scheduleNextReminder(task)
            }
        }
        
        return Result.success()
    }
    
    private fun showTaskReminderNotification(task: Task) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 为Android 8.0及以上版本创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = applicationContext.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建打开应用的Intent
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jodo_logo)
            .setContentTitle(applicationContext.getString(R.string.task_due_soon))
            .setContentText(task.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // 显示通知
        notificationManager.notify(task.id.toInt(), notification)
    }
    
    private suspend fun scheduleNextReminder(task: Task) {
        val nextDueDate = when (task.repeat) {
            takagicom.todo.jodo.model.RepeatInterval.DAILY -> task.due_date?.plusDays(1)
            takagicom.todo.jodo.model.RepeatInterval.WEEKLY -> task.due_date?.plusWeeks(1)
            takagicom.todo.jodo.model.RepeatInterval.MONTHLY -> task.due_date?.plusMonths(1)
            takagicom.todo.jodo.model.RepeatInterval.YEARLY -> task.due_date?.plusYears(1)
            else -> null
        } ?: return
        
        // 创建新的重复任务
        val repository = TaskRepository(applicationContext)
        val newTask = task.copy(
            id = repository.getNextId(),
            created_at = LocalDateTime.now(),
            due_date = nextDueDate,
            completed = false
        )
        repository.addTask(newTask)
        
        // 为新任务安排提醒
        scheduleReminder(applicationContext, newTask)
    }
    
    companion object {
        private const val CHANNEL_ID = "task_reminders"
        
        fun scheduleReminder(context: Context, task: Task) {
            if (!task.reminder || task.reminder_time == null) return
            
            val currentTime = LocalDateTime.now()
            val reminderTime = task.reminder_time
            
            // 计算从现在到提醒时间的延迟（以分钟为单位）
            val delayMinutes = ChronoUnit.MINUTES.between(currentTime, reminderTime)
            
            // 只安排未来的提醒
            if (delayMinutes <= 0) return
            
            // 创建工作请求数据
            val inputData = Data.Builder()
                .putLong("taskId", task.id)
                .build()
            
            // 创建工作请求
            val reminderWorkRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("reminder_${task.id}")
                .build()
            
            // 安排工作
            WorkManager.getInstance(context).enqueue(reminderWorkRequest)
        }
        
        fun cancelReminder(context: Context, taskId: Long) {
            WorkManager.getInstance(context).cancelAllWorkByTag("reminder_$taskId")
        }
    }
}
