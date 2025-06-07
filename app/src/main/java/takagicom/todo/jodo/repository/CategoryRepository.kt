package takagicom.todo.jodo.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import takagicom.todo.jodo.model.Category
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class CategoryRepository(private val context: Context) {
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val idCounter = AtomicLong(1)
    private val filename = "categories.json"
    
    init {
        loadCategories()
        
        _categories.value.maxByOrNull { it.id }?.let {
            idCounter.set(it.id + 1)
        }
    }
    
    fun getNextId(): Long {
        return idCounter.getAndIncrement()
    }
    
    suspend fun getCategoryById(categoryId: Long): Category? = withContext(Dispatchers.IO) {
        _categories.value.find { it.id == categoryId }
    }

    suspend fun addCategory(category: Category) = withContext(Dispatchers.IO) {
        val newCategories = _categories.value.toMutableList().apply {
            add(category)
        }
        _categories.value = newCategories
        saveCategories()
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        val newCategories = _categories.value.toMutableList().apply {
            val index = indexOfFirst { it.id == category.id }
            if (index != -1) {
                set(index, category)
            }
        }
        _categories.value = newCategories
        saveCategories()
    }
    
    suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        val newCategories = _categories.value.toMutableList().apply {
            removeAll { it.id == categoryId }
        }
        _categories.value = newCategories
        saveCategories()
    }
    
    suspend fun updateCategoriesOrder(reorderedCategories: List<Category>) = withContext(Dispatchers.IO) {
        _categories.value = reorderedCategories
        saveCategories()
    }
    
    private fun loadCategories() {
        try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                val json = file.readText()
                val gson = Gson()
                val typeToken = object : TypeToken<List<Category>>() {}.type
                val categoryList = gson.fromJson<List<Category>>(json, typeToken) ?: emptyList()
                _categories.value = categoryList
            }
        } catch (e: Exception) {
            _categories.value = emptyList()
        }
    }
    
    private suspend fun saveCategories() = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            val gson = Gson()
            val json = gson.toJson(_categories.value)
            file.writeText(json)
        } catch (e: Exception) {
            // 记录错误但不中断
        }
    }
}
