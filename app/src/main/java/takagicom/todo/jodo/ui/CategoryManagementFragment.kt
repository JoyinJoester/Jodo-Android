package takagicom.todo.jodo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.adapter.CategoryAdapter
import takagicom.todo.jodo.databinding.FragmentCategoryManagementBinding
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.viewmodel.CategoryViewModel
import takagicom.todo.jodo.viewmodel.TaskViewModel

class CategoryManagementFragment : Fragment() {    private var _binding: FragmentCategoryManagementBinding? = null
    private val binding get() = _binding!!

    private val categoryViewModel: CategoryViewModel by viewModels(
        ownerProducer = { requireActivity() }
    ) {
        SavedStateViewModelFactory(requireActivity().application, requireActivity())
    }
    
    private val taskViewModel: TaskViewModel by viewModels(
        ownerProducer = { requireActivity() }
    ) {
        SavedStateViewModelFactory(requireActivity().application, requireActivity())
    }

    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {        _binding = FragmentCategoryManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
        
        // 默认启用编辑模式以显示创建和删除功能
        categoryViewModel.setEditMode(true)
    }    private fun setupRecyclerView() {
        // 创建ItemTouchHelper用于拖拽排序和侧滑删除
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                
                val currentList = categoryAdapter.currentList.toMutableList()
                val item = currentList.removeAt(fromPosition)
                currentList.add(toPosition, item)
                
                categoryViewModel.reorderCategories(currentList)
                return true
            }            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val category = categoryAdapter.currentList[position]
                // 只有非默认分类才能被删除
                if (!category.isDefault) {
                    showDeleteConfirmDialog(category)
                } else {
                    // 如果是默认分类，恢复位置
                    categoryAdapter.notifyItemChanged(position)
                }
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                // 禁用自动长按拖动，改为手动触发
                return false
            }
              override fun isItemViewSwipeEnabled(): Boolean {
                return categoryViewModel.isEditMode.value == true
            }
        })
        
        // 创建CategoryAdapter
        categoryAdapter = CategoryAdapter(
            onCategoryClick = { category ->
                // 单击直接编辑分类
                showEditCategoryDialog(category)
            },            onCategoryLongClick = { _ ->
                // 长按不再弹出对话框，用于拖动排序
                // 这里可以添加拖动开始的视觉反馈，如轻微震动
            },
            showTaskCount = true,
            taskCountProvider = { categoryId ->
                getTaskCountForCategory(categoryId)
            },
            onStartDrag = { viewHolder ->
                // 手动启动拖动
                if (categoryViewModel.isEditMode.value == true) {
                    itemTouchHelper.startDrag(viewHolder)
                }
            }
        )
        
        // 设置RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
        
        // 附加ItemTouchHelper到RecyclerView
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupFab() {
        binding.fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }    private fun observeViewModel() {
        lifecycleScope.launch {
            categoryViewModel.categories.collect { categories ->
                categories.forEach { category ->
                }
                val sortedCategories = categories.sortedBy { it.orderIndex }
                categoryAdapter.submitList(sortedCategories)
            }
        }
        
        lifecycleScope.launch {
            categoryViewModel.isEditMode.collect { isEditMode ->
                binding.fabAddCategory.visibility = if (isEditMode) View.VISIBLE else View.GONE
                updateEditModeUI(isEditMode)
            }
        }
    }    private fun updateEditModeUI(@Suppress("UNUSED_PARAMETER") isEditMode: Boolean) {
        // 可以在这里更新UI状态，比如显示/隐藏编辑按钮等
        categoryAdapter.notifyDataSetChanged()
    }

    private fun getTaskCountForCategory(categoryId: Long): Int {
        return taskViewModel.tasks.value.count { 
            !it.deleted && it.categoryId == categoryId 
        }
    }

    private fun showAddCategoryDialog() {
        CategoryEditDialog.show(
            parentFragmentManager,
            null,
            onSave = { name, color, icon, description ->
                categoryViewModel.addCategory(name, color, icon, description)
            }
        )
    }

    private fun showEditCategoryDialog(category: Category) {
        if (category.isDefault) {
            // 默认分类不能编辑名称，只能编辑颜色和图标
            CategoryEditDialog.show(
                parentFragmentManager,
                category,
                isDefaultCategory = true,                onSave = { _, color, icon, description ->
                    val updatedCategory = category.copy(
                        color = color,
                        icon = icon,
                        description = description
                    )
                    categoryViewModel.updateCategory(updatedCategory)
                }
            )
        } else {
            CategoryEditDialog.show(
                parentFragmentManager,
                category,                onSave = { name, color, icon, description ->
                    val updatedCategory = category.copy(
                        name = name,
                        color = color,
                        icon = icon,
                        description = description
                    )
                    categoryViewModel.updateCategory(updatedCategory)
                }
            )        }
    }

    private fun showDeleteConfirmDialog(category: Category) {
        if (category.isDefault) return
        
        val taskCount = getTaskCountForCategory(category.id)
        val message = if (taskCount > 0) {
            "删除分类「${category.name}」会将其下的 $taskCount 个任务移动到「无分类」，确定要删除吗？"
        } else {
            "确定要删除分类「${category.name}」吗？"
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除分类")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                // 如果有任务，先将任务移动到无分类
                if (taskCount > 0) {
                    moveTasksToUncategorized(category.id)
                }
                categoryViewModel.deleteCategory(category.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun moveTasksToUncategorized(categoryId: Long) {
        val tasksToUpdate = taskViewModel.tasks.value.filter { 
            !it.deleted && it.categoryId == categoryId 
        }
        
        tasksToUpdate.forEach { task ->
            val updatedTask = task.copy(categoryId = 0L)
            taskViewModel.updateTask(updatedTask)
        }
    }

    fun setEditMode(enabled: Boolean) {
        categoryViewModel.setEditMode(enabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
