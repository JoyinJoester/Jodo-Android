package takagicom.todo.jodo.model

import java.time.LocalDateTime

/**
 * 任务分类数据模型
 */
data class Category(
    val id: Long,
    val name: String,
    val color: Int, // 分类颜色
    val icon: String = "ic_folder", // 分类图标
    val description: String = "", // 分类描述
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val orderIndex: Int = 0, // 排序索引
    val isDefault: Boolean = false // 是否为系统默认分类
) {
    companion object {
        // 预定义的默认分类
        val DEFAULT_CATEGORIES = listOf(
            Category(
                id = 1L,
                name = "工作",
                color = 0xFF2196F3.toInt(),
                icon = "ic_work",
                description = "工作相关任务",
                isDefault = true,
                orderIndex = 1
            ),
            Category(
                id = 2L,
                name = "个人",
                color = 0xFF4CAF50.toInt(),
                icon = "ic_person",
                description = "个人生活任务",
                isDefault = true,
                orderIndex = 2
            ),
            Category(
                id = 3L,
                name = "学习",
                color = 0xFFFF9800.toInt(),
                icon = "ic_school",
                description = "学习相关任务",
                isDefault = true,
                orderIndex = 3
            ),
            Category(
                id = 4L,
                name = "购物",
                color = 0xFFE91E63.toInt(),
                icon = "ic_shopping_cart",
                description = "购物清单",
                isDefault = true,
                orderIndex = 4
            ),
            Category(
                id = 5L,
                name = "健康",
                color = 0xFF9C27B0.toInt(),
                icon = "ic_favorite",
                description = "健康相关任务",
                isDefault = true,
                orderIndex = 5
            )
        )
        
        // 无分类的特殊分类
        val UNCATEGORIZED = Category(
            id = 0L,
            name = "无分类",
            color = 0xFF757575.toInt(),
            icon = "ic_folder_open",
            description = "未分类的任务",
            isDefault = true,
            orderIndex = 0
        )
    }
}
