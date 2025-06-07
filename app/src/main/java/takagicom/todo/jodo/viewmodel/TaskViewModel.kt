package takagicom.todo.jodo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import takagicom.todo.jodo.model.RepeatInterval
import takagicom.todo.jodo.model.RepeatSettings
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.model.TaskFilter
import takagicom.todo.jodo.repository.TaskRepository
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

class TaskViewModel(
    application: Application, 
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    
    private val repository = TaskRepository(application)
    private val isProcessing = AtomicBoolean(false)
    
    // 直接监听repository的数据变化
    val tasks = repository.tasks
    
    private val _filteredTasks = MutableStateFlow<List<Task>>(emptyList())
    val filteredTasks: StateFlow<List<Task>> = _filteredTasks.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()
    
    private val _currentCategoryId = MutableStateFlow<Long?>(null)
    val currentCategoryId: StateFlow<Long?> = _currentCategoryId.asStateFlow()
    
    init {
        // 初始时检查是否有保存的过滤器状态，否则使用默认值
        val savedFilterOrdinal = savedStateHandle.get<Int>(KEY_FILTER) ?: TaskFilter.ALL.ordinal
        val savedFilter = TaskFilter.values().getOrElse(savedFilterOrdinal) { TaskFilter.ALL }
        _currentFilter.value = savedFilter
        
        // 恢复分类过滤状态
        if (savedFilter == TaskFilter.CATEGORY) {
            val savedCategoryId = savedStateHandle.get<Long>(KEY_CATEGORY_ID)
            _currentCategoryId.value = savedCategoryId
        }
        
        // 监听数据变化，自动重新应用过滤器
        viewModelScope.launch {
            tasks.collect { allTasks ->
                applyCurrentFilter(allTasks)
            }
        }
        
        // 应用初始过滤器
        applyFilter(savedFilter)
    }
    
    fun addTask(
        description: String,
        dueDate: LocalDateTime? = null,
        starred: Boolean = false,
        reminder: Boolean = false,
        reminderTime: LocalDateTime? = null,
        repeatInterval: RepeatInterval = RepeatInterval.NONE,
        repeatSettings: RepeatSettings? = null,
        categoryId: Long = 0L
    ) {
        if (description.isBlank()) return
        viewModelScope.launch {
            val newId = repository.getNextId()
            val task = Task(
                id = newId,
                description = description,
                created_at = LocalDateTime.now(),
                due_date = dueDate,
                starred = starred,
                reminder = reminder,
                reminder_time = reminderTime,
                repeat = repeatInterval,
                repeatSettings = repeatSettings,
                categoryId = categoryId
            )
            repository.addTask(task)
        }
    }
    
    fun toggleTaskCompleted(taskId: Long) {
        if (isProcessing.getAndSet(true)) return
          viewModelScope.launch {
            try {
                val currentTasks = repository.tasks.value
                val task = currentTasks.find { it.id == taskId }
                if (task != null) {
                    val updatedTask = task.copy(completed = !task.completed)
                    repository.updateTask(updatedTask)
                }
            } catch (e: Exception) {
                // 记录错误但不中断
            } finally {
                isProcessing.set(false)
            }
        }
    }
      fun toggleTaskStarred(taskId: Long) {
        if (isProcessing.getAndSet(true)) {
            return
        }
        
        viewModelScope.launch {
            try {
                val currentTasks = repository.tasks.value
                val task = currentTasks.find { it.id == taskId }
                if (task != null) {
                    val updatedTask = task.copy(starred = !task.starred)
                    repository.updateTask(updatedTask)
                }
            } catch (e: Exception) {
                // 记录错误但不中断
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    fun deleteTask(taskId: Long) {
        if (isProcessing.getAndSet(true)) return
        
        viewModelScope.launch {            try {
                repository.deleteTask(taskId)
            } catch (e: Exception) {
                // 记录错误但不中断
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    fun updateTask(task: Task) {
        if (isProcessing.getAndSet(true)) return
        viewModelScope.launch {
            try {
                repository.updateTask(task)
            } finally {
                isProcessing.set(false)
            }
        }
    }
      fun applyFilter(filter: TaskFilter) {
        viewModelScope.launch {
            _currentFilter.value = filter
            savedStateHandle[KEY_FILTER] = filter.ordinal
            
            // 如果不是分类过滤，清除分类ID
            if (filter != TaskFilter.CATEGORY) {
                _currentCategoryId.value = null
                savedStateHandle.remove<Long>(KEY_CATEGORY_ID)
            }
            
            // 使用当前的数据重新应用过滤器
            applyCurrentFilter(tasks.value)
        }
    }
      fun applyFilterByCategory(categoryId: Long) {
        viewModelScope.launch {
            // 先设置分类ID，再设置过滤器，确保观察者能正确获取到分类信息
            _currentCategoryId.value = categoryId
            savedStateHandle[KEY_CATEGORY_ID] = categoryId
            _currentFilter.value = TaskFilter.CATEGORY
            savedStateHandle[KEY_FILTER] = TaskFilter.CATEGORY.ordinal
            
            // 使用当前的数据重新应用过滤器
            applyCurrentFilter(tasks.value)
        }
    }
    
    private suspend fun applyCurrentFilter(allTasks: List<Task>) {
        withContext(Dispatchers.Default) {
            val filteredTasks = allTasks.filter { !it.deleted }
                .let { tasks ->
                    when (_currentFilter.value) {
                        TaskFilter.ALL -> tasks
                        TaskFilter.ACTIVE -> tasks.filter { !it.completed }
                        TaskFilter.COMPLETED -> tasks.filter { it.completed }
                        TaskFilter.STARRED -> tasks.filter { it.starred }
                        TaskFilter.DUE_TODAY -> {
                            val today = LocalDateTime.now()
                            tasks.filter { task ->
                                // 显示所有有截止日期且未过期的任务（包括今天及以后）
                                task.due_date?.let { dueDate ->
                                    !dueDate.toLocalDate().isBefore(today.toLocalDate())
                                } ?: false
                            }
                        }                        TaskFilter.CATEGORY -> {
                            val categoryId = _currentCategoryId.value ?: 0L
                            tasks.filter { it.categoryId == categoryId }
                        }
                    }
                }
            
            // 应用排序规则: 星标置顶，已完成置底
            val sortedTasks = filteredTasks.sortedWith(
                compareBy<Task> { it.completed }  // 未完成的在前面
                    .thenByDescending { it.starred }  // 星标的在前面
                    .thenByDescending { it.created_at }  // 新创建的在前面
            )
            
            _filteredTasks.value = sortedTasks
        }
    }
    
    fun getTaskById(taskId: Long): Task? {
        return repository.tasks.value.find { it.id == taskId }
    }
    
    fun clearFilter() {
        viewModelScope.launch {
            _currentFilter.value = TaskFilter.ALL
            _currentCategoryId.value = null
            savedStateHandle.remove<Int>(KEY_FILTER)
            savedStateHandle.remove<Long>(KEY_CATEGORY_ID)
            
            // 重新应用过滤器
            applyCurrentFilter(tasks.value)
        }
    }

    companion object {
        private const val KEY_FILTER = "current_filter"
        private const val KEY_CATEGORY_ID = "current_category_id"
    }
}
