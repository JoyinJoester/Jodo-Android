package takagicom.todo.jodo

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.SubMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import takagicom.todo.jodo.databinding.ActivityMainBinding
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.model.TaskFilter
import takagicom.todo.jodo.utils.PermissionHelper
import takagicom.todo.jodo.viewmodel.CategoryViewModel
import takagicom.todo.jodo.viewmodel.TaskViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    // 添加 ViewModels
    private val categoryViewModel: CategoryViewModel by viewModels {
        SavedStateViewModelFactory(application, this)
    }
    
    private val taskViewModel: TaskViewModel by viewModels {
        SavedStateViewModelFactory(application, this)
    }
    
    // 动态分类菜单项的基础ID
    private    companion object {
        private const val CATEGORY_MENU_BASE_ID = 10000
    }

    // 存储菜单ID到分类ID的映射
    private val menuIdToCategoryId = mutableMapOf<Int, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 使用更安全的状态栏沉浸式方法
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)

        // 配置顶部 ActionBar 与导航抽屉关联 (移除了搜索选项)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_all_tasks, R.id.nav_active_tasks, R.id.nav_completed_tasks,
                R.id.nav_starred_tasks, R.id.nav_today_tasks, R.id.nav_statistics,
                R.id.nav_category_management            ), drawerLayout        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // 不使用 setupWithNavController，完全使用自定义导航逻辑
        // navView.setupWithNavController(navController)
        
        // 设置导航菜单点击监听器以处理动态分类项和过滤器
        setupNavigationClickListener(navView)
        
        // 设置侧边栏打开监听器，确保每次打开时都刷新选中状态
        setupDrawerListener()
        
        // 观察分类数据变化并更新菜单
        observeCategoriesAndUpdateMenu(navView)
        
        // 设置导航目标变化监听器，直接控制标题
        setupNavigationDestinationListener()
        
        // 观察任务过滤器状态变化并更新侧边栏选中状态
        observeTaskFilterAndUpdateSidebar()
          // 检查并请求通知权限
        checkNotificationPermission()
        
        // 处理从小组件传递的Intent
        handleWidgetIntent()
    }
    
    /**
     * 设置导航菜单点击监听器
     */
    private fun setupNavigationClickListener(navView: NavigationView) {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_all_tasks -> {
                    handleStandardMenuSelection(TaskFilter.ALL, getString(R.string.menu_all))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_active_tasks -> {
                    handleStandardMenuSelection(TaskFilter.ACTIVE, getString(R.string.menu_active))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_completed_tasks -> {
                    handleStandardMenuSelection(TaskFilter.COMPLETED, getString(R.string.menu_completed))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_starred_tasks -> {
                    handleStandardMenuSelection(TaskFilter.STARRED, getString(R.string.menu_starred))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_today_tasks -> {
                    handleStandardMenuSelection(TaskFilter.DUE_TODAY, getString(R.string.menu_today))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                else -> {
                    // 添加调试日志捕获所有菜单点击
                    
                    // 检查是否是分类菜单项
                    val categoryId = menuIdToCategoryId[menuItem.itemId]
                    if (categoryId != null) {
                        handleCategorySelection(categoryId)
                        binding.drawerLayout.closeDrawers()
                        true
                    } else {
                        // 对于其他Fragment（统计、设置等），使用正常导航
                        try {
                            navController.navigate(menuItem.itemId)
                            binding.drawerLayout.closeDrawers()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 处理分类选择
     */
    private fun handleCategorySelection(categoryId: Long) {
        taskViewModel.applyFilterByCategory(categoryId)
        navController.navigate(R.id.nav_all_tasks)
        // 标题将由观察者自动更新
    }

    /**
     * 处理标准菜单项选择（全部任务、未完成等）
     */
    private fun handleStandardMenuSelection(filter: TaskFilter, title: String) {
        taskViewModel.applyFilter(filter)
        navController.navigate(R.id.nav_all_tasks)
        // 标题将由观察者自动更新
    }
    
    /**
     * 观察分类数据变化并更新菜单
     */
    private fun observeCategoriesAndUpdateMenu(navView: NavigationView) {
        lifecycleScope.launch {
            categoryViewModel.categories.collect { categories ->
                categories.forEach { category ->
                }
                updateCategoryMenu(navView.menu, categories)
            }
        }
    }
    
    /**
     * 设置导航目标变化监听器，直接控制标题
     */
    private fun setupNavigationDestinationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            
            // 根据导航目标设置标题
            when (destination.id) {
                R.id.nav_all_tasks -> {
                    // 对于AllTasksFragment，根据当前过滤器设置标题
                    updateTitleBasedOnCurrentFilter()
                }
                R.id.nav_statistics -> {
                    supportActionBar?.title = getString(R.string.statistics)
                }
                R.id.nav_category_management -> {
                    supportActionBar?.title = getString(R.string.category_management)
                }
                else -> {
                    // 使用默认标题
                    destination.label?.let { label ->
                        supportActionBar?.title = label
                    }
                }
            }
        }
    }
    
    /**
     * 根据当前过滤器状态更新标题
     */
    private fun updateTitleBasedOnCurrentFilter() {
        val currentFilter = taskViewModel.currentFilter.value
        val currentCategoryId = taskViewModel.currentCategoryId.value
        
        
        when (currentFilter) {
            TaskFilter.CATEGORY -> {
                if (currentCategoryId != null) {
                    // 同步获取分类名称
                    val categories = categoryViewModel.categories.value
                    val category = categories.find { it.id == currentCategoryId }
                    if (category != null) {
                        supportActionBar?.title = category.name
                    } else {
                        supportActionBar?.title = getString(R.string.menu_all)
                    }
                } else {
                    supportActionBar?.title = getString(R.string.menu_all)
                }
            }
            TaskFilter.ALL -> supportActionBar?.title = getString(R.string.menu_all)
            TaskFilter.ACTIVE -> supportActionBar?.title = getString(R.string.menu_active)
            TaskFilter.COMPLETED -> supportActionBar?.title = getString(R.string.menu_completed)
            TaskFilter.STARRED -> supportActionBar?.title = getString(R.string.menu_starred)
            TaskFilter.DUE_TODAY -> supportActionBar?.title = getString(R.string.menu_today)
        }
    }

    /**
     * 观察任务过滤器状态变化并更新侧边栏选中状态
     */
    private fun observeTaskFilterAndUpdateSidebar() {
        // 只监听过滤器变化来更新侧边栏选中状态，标题由导航监听器处理
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                taskViewModel.currentFilter,
                taskViewModel.currentCategoryId
            ) { filter, categoryId ->
                Pair(filter, categoryId)
            }.collect { (filter, categoryId) ->
                
                // 更新侧边栏选中状态
                binding.navView.post {
                    updateNavigationMenuSelection(filter, categoryId)
                }
                
                // 如果当前在AllTasksFragment，立即更新标题
                if (navController.currentDestination?.id == R.id.nav_all_tasks) {
                    updateTitleBasedOnCurrentFilter()
                }
            }
        }
    }
    
    /**
     * 更新分类菜单
     */
    private fun updateCategoryMenu(menu: Menu, categories: List<Category>) {
        
        // 查找分类子菜单
        val categoryMenuItem = menu.findItem(R.id.nav_categories) 
            ?: menu.add(0, R.id.nav_categories, 0, getString(R.string.categories))
        
        val categorySubMenu = categoryMenuItem.subMenu 
            ?: menu.addSubMenu(0, R.id.nav_categories, 0, getString(R.string.categories))
        
        // 清除现有的分类项和映射
        categorySubMenu.clear()
        menuIdToCategoryId.clear()
        
        if (categories.isEmpty()) {
            // 如果没有分类，添加提示项
            val emptyItem = categorySubMenu.add(1, -1, 0, "暂无分类")
            emptyItem.isEnabled = false
            emptyItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_folder_24)
        } else {
            // 添加分类项到正确的组中
            categories.sortedBy { it.orderIndex }.forEachIndexed { index, category ->
                val menuId = CATEGORY_MENU_BASE_ID + category.id.toInt()
                // 使用组ID 1 来确保分类菜单项在正确的组中
                val menuItem = categorySubMenu.add(1, menuId, index, category.name)
                
                // 将映射关系保存到映射表
                menuIdToCategoryId[menuId] = category.id
                
                // 设置分类图标
                try {
                    val iconRes = resources.getIdentifier(
                        category.icon, 
                        "drawable", 
                        packageName
                    )
                    if (iconRes != 0) {
                        menuItem.icon = ContextCompat.getDrawable(this, iconRes)
                    } else {
                        menuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_folder_24)
                    }
                } catch (e: Exception) {
                    menuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_folder_24)
                }
                
                // 设置任务数量作为副标题
                val taskCount = taskViewModel.tasks.value.count { 
                    !it.deleted && it.categoryId == category.id 
                }
                if (taskCount > 0) {
                    menuItem.title = "${category.name} ($taskCount)"
                }
            }
        }
        
        // 设置分类组为单选
        categorySubMenu.setGroupCheckable(1, true, false)
    }
    
    /**
     * 检查通知权限，如果需要则请求
     */
    private fun checkNotificationPermission() {
        if (!PermissionHelper.hasNotificationPermission(this)) {
            if (PermissionHelper.shouldShowNotificationPermissionRationale(this)) {
                // 显示权限说明
                Toast.makeText(
                    this,
                    "应用需要通知权限来发送任务提醒",
                    Toast.LENGTH_LONG
                ).show()
            }
            // 请求权限
            PermissionHelper.requestNotificationPermission(this)
        }
    }
    
    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        PermissionHelper.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onPermissionGranted = {
                Toast.makeText(this, "通知权限已授予，现在可以接收任务提醒", Toast.LENGTH_SHORT).show()
            },
            onPermissionDenied = {
                Toast.makeText(this, "通知权限被拒绝，无法接收任务提醒", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    /**
     * 更新侧边栏菜单的选中状态
     */
    private fun updateNavigationMenuSelection(filter: TaskFilter, categoryId: Long? = null) {
        val navView = binding.navView
        val menu = navView.menu
        
        
        // 首先清除所有选中状态
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isChecked = false
            
            // 清除子菜单项的选中状态
            item.subMenu?.let { subMenu ->
                for (j in 0 until subMenu.size()) {
                    subMenu.getItem(j).isChecked = false
                }
            }
        }
        
        // 根据当前过滤器设置正确的选中状态
        when (filter) {
            TaskFilter.ALL -> {
                menu.findItem(R.id.nav_all_tasks)?.let { item ->
                    item.isChecked = true
                }
            }
            TaskFilter.ACTIVE -> {
                menu.findItem(R.id.nav_active_tasks)?.let { item ->
                    item.isChecked = true
                }
            }
            TaskFilter.COMPLETED -> {
                menu.findItem(R.id.nav_completed_tasks)?.let { item ->
                    item.isChecked = true
                }
            }
            TaskFilter.STARRED -> {
                menu.findItem(R.id.nav_starred_tasks)?.let { item ->
                    item.isChecked = true
                }
            }
            TaskFilter.DUE_TODAY -> {
                menu.findItem(R.id.nav_today_tasks)?.let { item ->
                    item.isChecked = true
                }
            }            TaskFilter.CATEGORY -> {
                // 找到对应的分类菜单项并设置为选中
                categoryId?.let { id ->
                    val categoryMenuId = menuIdToCategoryId.entries.find { it.value == id }?.key
                    categoryMenuId?.let { menuId ->
                        menu.findItem(menuId)?.let { item ->
                            item.isChecked = true
                        }
                    }
                }
            }
        }
        
        // 强制刷新NavigationView
        navView.invalidate()
    }
    
    /**
     * 设置侧边栏打开监听器
     */
    private fun setupDrawerListener() {
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {
                // 不需要处理
            }

            override fun onDrawerOpened(drawerView: android.view.View) {
                // 每次打开侧边栏时，强制刷新选中状态
                val currentFilter = taskViewModel.currentFilter.value
                val currentCategoryId = taskViewModel.currentCategoryId.value
                
                
                // 立即更新侧边栏选中状态
                updateNavigationMenuSelection(currentFilter, currentCategoryId)
            }

            override fun onDrawerClosed(drawerView: android.view.View) {
                // 不需要处理
            }            override fun onDrawerStateChanged(newState: Int) {
                // 不需要处理
            }        })
    }
    
    /**
     * 处理从小组件传递的Intent
     */
    private fun handleWidgetIntent() {
        intent?.let { intent ->
            val action = intent.getStringExtra("action")
            when (action) {
                "add_task" -> {
                    // 导航到添加任务页面
                    navController.navigate(R.id.nav_all_tasks)
                    // 这里可以触发添加任务的对话框或页面
                    // 由于没有看到具体的添加任务实现，这里只是示例
                }
                "edit_task" -> {
                    // 处理编辑任务
                    val taskId = intent.getLongExtra("task_id", -1)
                    if (taskId != -1L) {
                        // 导航到对应的任务编辑页面
                        navController.navigate(R.id.nav_all_tasks)
                        // 这里可以打开编辑任务的对话框
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent()
    }
  
}
