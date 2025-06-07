package takagicom.todo.jodo.model

import java.time.DayOfWeek
import java.time.LocalDateTime

data class Task(
    val id: Long,
    val description: String,
    val completed: Boolean = false,
    val created_at: LocalDateTime = LocalDateTime.now(),
    val due_date: LocalDateTime? = null,
    val starred: Boolean = false,
    val deleted: Boolean = false,
    val reminder: Boolean = false,       // 是否开启提醒
    val reminder_time: LocalDateTime? = null, // 提醒时间
    val repeat: RepeatInterval = RepeatInterval.NONE, // 重复间隔
    val repeatSettings: RepeatSettings? = null, // 高级重复设置
    val categoryId: Long = 0L           // 分类ID，0表示无分类
)

enum class RepeatInterval {
    NONE,       // 不重复
    DAILY,      // 每天
    WEEKLY,     // 每周
    MONTHLY,    // 每月
    YEARLY      // 每年
}

/**
 * 重复设置详细配置
 */
data class RepeatSettings(
    // 每周重复时选择的星期几 (1=周一, 7=周日)
    val weeklyDays: Set<Int> = emptySet(),
    
    // 每月重复时选择的日期 (1-31)
    val monthlyDay: Int? = null,
    
    // 每年重复时的月份和日期
    val yearlyMonth: Int? = null,  // 1-12
    val yearlyDay: Int? = null,    // 1-31
    
    // 重复间隔 (例如每2天、每3周等)
    val interval: Int = 1,
    
    // 重复结束条件
    val endDate: LocalDateTime? = null,
    val maxOccurrences: Int? = null
)
