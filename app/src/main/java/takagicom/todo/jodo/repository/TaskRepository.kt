package takagicom.todo.jodo.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.model.RepeatInterval
import takagicom.todo.jodo.model.RepeatSettings
// import takagicom.todo.jodo.worker.ReminderWorker
import java.io.File
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

class TaskRepository(private val context: Context) {
    
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    private val idCounter = AtomicLong(0)
    private val filename = "tasks.json"
    
    init {
        loadTasks()
        
        tasks.value.maxByOrNull { it.id }?.let {
            idCounter.set(it.id + 1)
        }
    }
    
    fun getNextId(): Long {
        return idCounter.getAndIncrement()
    }
    
    fun getTaskById(taskId: Long): Task? {
        return _tasks.value.find { it.id == taskId }
    }
    
    suspend fun addTask(task: Task) = withContext(Dispatchers.IO) {
        val newTasks = _tasks.value.toMutableList().apply {
            add(task)
        }
        _tasks.value = newTasks
        saveTasks()
          if (task.reminder && task.reminder_time != null) {
            // ReminderWorker.scheduleTaskReminder(context, task)
            // TODO: 实现提醒功能
        }
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        try {
            val currentTasks = _tasks.value
            val oldTask = currentTasks.find { it.id == task.id }
            if (oldTask == null) {
                return@withContext
            }
            
            val newTasks = currentTasks.toMutableList().apply {
                val index = indexOfFirst { it.id == task.id }
                if (index != -1) {
                    set(index, task)
                } else {
                    return@withContext
                }
            }
            
            _tasks.value = newTasks
            saveTasks()
            
        } catch (e: Exception) {
            // 记录错误但不中断
        }
    }
    
    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        val currentTask = _tasks.value.find { it.id == taskId }
        
        val newTasks = _tasks.value.toMutableList().apply {
            val index = indexOfFirst { it.id == taskId }
            if (index != -1) {
                val task = get(index)
                set(index, task.copy(deleted = true))
            }
        }
        _tasks.value = newTasks
        saveTasks()
          currentTask?.let {
            if (it.reminder && it.reminder_time != null) {
                // ReminderWorker.cancelReminder(context, it.id)
                // TODO: 实现取消提醒功能
            }
        }
    }
      
    private fun loadTasks() {
        try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                val json = file.readText()
                val gson = createGson()
                val typeToken = object : TypeToken<List<Task>>() {}.type
                val taskList = gson.fromJson<List<Task>>(json, typeToken) ?: emptyList()
                _tasks.value = taskList
            }
        } catch (e: Exception) {
            _tasks.value = emptyList()
        }
    }
      
    private suspend fun saveTasks() = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            val gson = createGson()
            val tasksToSave = _tasks.value
            val json = gson.toJson(tasksToSave)
            file.writeText(json)
        } catch (e: Exception) {
            // 记录错误但不中断
        }
    }
    
    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(Task::class.java, TaskDeserializer())
            .registerTypeAdapter(RepeatSettings::class.java, RepeatSettingsAdapter())
            .create()
    }
    
    class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter) ?: "")
        }
          
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
            val dateString = json?.asString
            return if (dateString.isNullOrEmpty()) {
                null
            } else {
                try {
                    LocalDateTime.parse(dateString, formatter)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    class TaskDeserializer : JsonDeserializer<Task> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Task? {
            if (json == null || !json.isJsonObject) return null
            
            val jsonObject = json.asJsonObject
            
            return try {
                Task(
                    id = jsonObject.get("id")?.asLong ?: 0L,
                    description = jsonObject.get("description")?.asString ?: "",
                    completed = jsonObject.get("completed")?.asBoolean ?: false,
                    created_at = context?.deserialize(jsonObject.get("created_at"), LocalDateTime::class.java) ?: LocalDateTime.now(),
                    due_date = context?.deserialize(jsonObject.get("due_date"), LocalDateTime::class.java),
                    starred = jsonObject.get("starred")?.asBoolean ?: false,
                    deleted = jsonObject.get("deleted")?.asBoolean ?: false,
                    reminder = jsonObject.get("reminder")?.asBoolean ?: false,
                    reminder_time = context?.deserialize(jsonObject.get("reminder_time"), LocalDateTime::class.java),
                    repeat = try {
                        RepeatInterval.valueOf(jsonObject.get("repeat")?.asString ?: "NONE")
                    } catch (e: Exception) {
                        RepeatInterval.NONE
                    },
                    repeatSettings = context?.deserialize(jsonObject.get("repeatSettings"), RepeatSettings::class.java),
                    categoryId = jsonObject.get("categoryId")?.asLong ?: 0L
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    class RepeatSettingsAdapter : JsonSerializer<RepeatSettings>, JsonDeserializer<RepeatSettings> {
        override fun serialize(src: RepeatSettings?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            if (src == null) return JsonPrimitive("")
            
            val gson = Gson()
            return gson.toJsonTree(src)
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): RepeatSettings? {
            if (json == null || json.isJsonNull || (json.isJsonPrimitive && json.asString.isEmpty())) {
                return null
            }
            
            return try {
                val gson = GsonBuilder()
                    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
                    .create()
                gson.fromJson(json, RepeatSettings::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
