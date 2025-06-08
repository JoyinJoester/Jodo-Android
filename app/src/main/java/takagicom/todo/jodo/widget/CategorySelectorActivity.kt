package takagicom.todo.jodo.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import kotlinx.coroutines.*
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.repository.CategoryRepository
import takagicom.todo.jodo.repository.TaskRepository

// 筛选类型枚举
enum class FilterType {
    ALL, IN_PROGRESS, COMPLETED, STARRED, CATEGORY
}

// 筛选项数据类
data class FilterItem(
    val type: FilterType,
    val categoryId: Long? = null,
    val name: String,
    val description: String,
    val count: Int,
    val color: Int? = null,
    val icon: Int
)

class CategorySelectorActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var loadingText: TextView
    private lateinit var filterScroll: ScrollView
    private lateinit var filterOptions: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var backgroundOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置状态栏和导航栏透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        setContentView(R.layout.activity_category_selector)
        
        Log.d("CategorySelector", "Activity created")
        
        initViews()
        loadFiltersAsync()
    }

    private fun initViews() {
        loadingText = findViewById(R.id.loading_text)
        filterScroll = findViewById(R.id.filter_scroll)
        filterOptions = findViewById(R.id.filter_options)
        emptyText = findViewById(R.id.empty_text)
        closeButton = findViewById(R.id.close_button)
        backgroundOverlay = findViewById(R.id.background_overlay)
        
        // 设置按钮点击事件
        closeButton.setOnClickListener { finish() }
        backgroundOverlay.setOnClickListener { finish() }
    }

    private fun loadFiltersAsync() {
        scope.launch {
            try {
                Log.d("CategorySelector", "Starting to load filters")
                
                val categoryRepository = CategoryRepository(this@CategorySelectorActivity)
                val taskRepository = TaskRepository(this@CategorySelectorActivity)
                
                // 等待数据加载完成
                delay(500)
                
                val categories = categoryRepository.categories.value
                val tasks = taskRepository.tasks.value
                
                Log.d("CategorySelector", "Loaded ${categories.size} categories and ${tasks.size} tasks")
                
                // 创建筛选选项
                val filterItems = createFilterItems(categories, tasks)
                
                if (filterItems.isEmpty()) {
                    showEmptyState()
                } else {
                    showFilterOptions(filterItems)
                }
                
            } catch (e: Exception) {
                Log.e("CategorySelector", "Error loading filters", e)
                showEmptyState()
            }
        }
    }

    private fun createFilterItems(categories: List<Category>, tasks: List<takagicom.todo.jodo.model.Task>): List<FilterItem> {
        val items = mutableListOf<FilterItem>()
        
        // 添加基本筛选选项
        val allTasksCount = tasks.count { !it.completed }
        val inProgressCount = tasks.count { !it.completed }
        val completedCount = tasks.count { it.completed }
        val starredCount = tasks.count { it.starred && !it.completed }
        
        items.add(FilterItem(
            type = FilterType.ALL,
            name = "全部任务",
            description = "显示所有任务",
            count = allTasksCount,
            icon = R.drawable.ic_folder
        ))
        
        items.add(FilterItem(
            type = FilterType.IN_PROGRESS,
            name = "进行中",
            description = "未完成的任务",
            count = inProgressCount,
            icon = R.drawable.ic_folder
        ))
        
        items.add(FilterItem(
            type = FilterType.COMPLETED,
            name = "已完成",
            description = "已完成的任务",
            count = completedCount,
            icon = R.drawable.ic_folder
        ))
        
        items.add(FilterItem(
            type = FilterType.STARRED,
            name = "收藏",
            description = "收藏的任务",
            count = starredCount,
            icon = R.drawable.ic_folder
        ))
        
        // 添加分类筛选选项
        if (categories.isNotEmpty()) {
            categories.forEach { category ->
                val categoryTaskCount = tasks.count { it.categoryId == category.id && !it.completed }
                items.add(FilterItem(
                    type = FilterType.CATEGORY,
                    categoryId = category.id,
                    name = category.name,
                    description = "分类：${category.name}",
                    count = categoryTaskCount,
                    color = category.color,
                    icon = R.drawable.ic_folder
                ))
            }
        }
        
        return items
    }

    private fun showEmptyState() {
        loadingText.visibility = View.GONE
        filterScroll.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = "暂无筛选选项\n请先在主应用中创建任务和分类"
    }

    private fun showFilterOptions(filterItems: List<FilterItem>) {
        loadingText.visibility = View.GONE
        filterScroll.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        
        // 清空之前的选项
        filterOptions.removeAllViews()
        
        // 添加筛选选项
        filterItems.forEach { item ->
            val itemView = createFilterItemView(item)
            filterOptions.addView(itemView)
        }
    }

    private fun createFilterItemView(item: FilterItem): View {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.filter_option_item, filterOptions, false)
        
        val filterIcon = itemView.findViewById<ImageView>(R.id.filter_icon)
        val filterName = itemView.findViewById<TextView>(R.id.filter_name)
        val filterDescription = itemView.findViewById<TextView>(R.id.filter_description)
        val taskCount = itemView.findViewById<TextView>(R.id.task_count)
        
        // 设置内容
        filterName.text = item.name
        filterDescription.text = item.description
        taskCount.text = item.count.toString()
        
        // 设置图标和颜色
        if (item.type == FilterType.CATEGORY && item.color != null) {
            try {
                filterIcon.setColorFilter(item.color)
            } catch (e: Exception) {
                filterIcon.setColorFilter(Color.parseColor("#2196F3"))
            }
        }
        
        // 设置点击事件
        itemView.setOnClickListener {
            handleFilterSelection(item)
        }
        
        return itemView
    }

    private fun handleFilterSelection(item: FilterItem) {
        Log.d("CategorySelector", "Selected filter: ${item.name} (type: ${item.type})")
        
        when (item.type) {
            FilterType.ALL -> updateWidgetFilter(null, "all")
            FilterType.IN_PROGRESS -> updateWidgetFilter(null, "in_progress") 
            FilterType.COMPLETED -> updateWidgetFilter(null, "completed")
            FilterType.STARRED -> updateWidgetFilter(null, "starred")
            FilterType.CATEGORY -> updateWidgetFilter(item.categoryId, "category")
        }
        
        finish()
    }

    private fun updateWidgetFilter(categoryId: Long?, filterType: String) {
        Log.d("CategorySelector", "Updating widget filter to categoryId: $categoryId, type: $filterType")
        
        // 保存选择的筛选设置到SharedPreferences
        val prefs = getSharedPreferences("widget_preferences", MODE_PRIVATE)
        prefs.edit().apply {
            putString("filter_type", filterType)
            if (categoryId != null) {
                putLong("selected_category_id", categoryId)
                putBoolean("category_filter_enabled", true)
                Log.d("CategorySelector", "Enabled category filter for categoryId: $categoryId")
            } else {
                remove("selected_category_id")
                putBoolean("category_filter_enabled", false)
                Log.d("CategorySelector", "Set filter type: $filterType")
            }
            apply()
        }
        
        // 触发小组件更新
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val thisWidget = ComponentName(this, SimpleTaskWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        
        Log.d("CategorySelector", "Updating ${appWidgetIds.size} widgets")
        
        val updateIntent = Intent(this, SimpleTaskWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        sendBroadcast(updateIntent)
        
        // 同时发送数据变更广播
        val dataChangeIntent = Intent("TASK_DATA_CHANGED")
        sendBroadcast(dataChangeIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
