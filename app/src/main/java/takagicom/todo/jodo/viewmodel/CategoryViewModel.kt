package takagicom.todo.jodo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.repository.CategoryRepository
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = CategoryRepository(application)
    private val isProcessing = AtomicBoolean(false)
    
    // 直接监听repository的数据变化
    val categories = repository.categories
    
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    fun getAllCategoriesWithUncategorized(): List<Category> {
        val currentCategories = categories.value
        return listOf(Category.UNCATEGORIZED) + currentCategories.sortedBy { it.orderIndex }
    }
      fun getCategoryById(categoryId: Long): Category? {
        return runBlocking {
            repository.getCategoryById(categoryId)
        }
    }
      fun addCategory(
        name: String,
        color: Int,
        icon: String = "ic_folder",
        description: String = ""
    ) {
        if (isProcessing.getAndSet(true)) return
        if (name.isBlank()) return
        
        
        viewModelScope.launch {
            try {
                val newId = repository.getNextId()
                val maxOrder = categories.value.maxOfOrNull { it.orderIndex } ?: 0
                val category = Category(
                    id = newId,
                    name = name,
                    color = color,
                    icon = icon,
                    description = description,
                    createdAt = LocalDateTime.now(),
                    orderIndex = maxOrder + 1
                )
                repository.addCategory(category)
            } finally {
                isProcessing.set(false)
            }
        }
    }
      fun updateCategory(category: Category) {
        if (isProcessing.getAndSet(true)) return
        
        
        viewModelScope.launch {
            try {
                repository.updateCategory(category)
                if (_selectedCategory.value?.id == category.id) {
                    _selectedCategory.value = category
                }
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    fun deleteCategory(categoryId: Long) {
        if (isProcessing.getAndSet(true)) return
        
        viewModelScope.launch {
            try {
                repository.deleteCategory(categoryId)
                if (_selectedCategory.value?.id == categoryId) {
                    _selectedCategory.value = null
                }
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    fun reorderCategories(newOrderList: List<Category>) {
        if (isProcessing.getAndSet(true)) return
        
        viewModelScope.launch {
            try {
                val reorderedList = newOrderList.mapIndexed { index, category ->
                    category.copy(orderIndex = index + 1)
                }
                repository.updateCategoriesOrder(reorderedList)
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }
    
    fun setEditMode(isEdit: Boolean) {
        _isEditMode.value = isEdit
        if (!isEdit) {
            _selectedCategory.value = null
        }
    }
    
    // 预定义颜色选项
    fun getAvailableColors(): List<Int> {
        return listOf(
            0xFF2196F3.toInt(), // 蓝色
            0xFF4CAF50.toInt(), // 绿色
            0xFFFF9800.toInt(), // 橙色
            0xFFE91E63.toInt(), // 粉色
            0xFF9C27B0.toInt(), // 紫色
            0xFFF44336.toInt(), // 红色
            0xFF00BCD4.toInt(), // 青色
            0xFF8BC34A.toInt(), // 浅绿色
            0xFFFFEB3B.toInt(), // 黄色
            0xFF795548.toInt(), // 棕色
            0xFF607D8B.toInt(), // 蓝灰色
            0xFF9E9E9E.toInt()  // 灰色
        )
    }
    
    // 预定义图标选项
    fun getAvailableIcons(): List<String> {
        return listOf(
            "ic_work",
            "ic_person",
            "ic_school",
            "ic_shopping_cart",
            "ic_favorite",
            "ic_home",
            "ic_sports",
            "ic_restaurant",
            "ic_local_hospital",
            "ic_directions_car",
            "ic_flight",
            "ic_music_note",
            "ic_camera",
            "ic_pets",
            "ic_eco",
            "ic_build"
        )
    }
}
