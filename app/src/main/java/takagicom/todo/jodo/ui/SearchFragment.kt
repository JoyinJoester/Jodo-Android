package takagicom.todo.jodo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.adapter.TaskAdapter
import takagicom.todo.jodo.databinding.FragmentSearchBinding
import takagicom.todo.jodo.model.RepeatInterval
import takagicom.todo.jodo.model.RepeatSettings
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.viewmodel.TaskViewModel

class SearchFragment : Fragment() {
    
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    // 使用Activity级别的共享ViewModel，确保与其他Fragment同步
    private val viewModel: TaskViewModel by viewModels(
        ownerProducer = { requireActivity() }
    ) {
        SavedStateViewModelFactory(requireActivity().application, requireActivity())
    }
    
    // 分类ViewModel
    private val categoryViewModel: takagicom.todo.jodo.viewmodel.CategoryViewModel by viewModels(
        ownerProducer = { requireActivity() }
    ) {
        SavedStateViewModelFactory(requireActivity().application, requireActivity())
    }
    
    private lateinit var taskAdapter: TaskAdapter
    private var searchQuery: String = ""
    
    // 分类缓存
    private val categoryCache = mutableMapOf<Long, String>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupSearchView()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupSearchView() {
        binding.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchQuery = query?.trim() ?: ""
                    performSearch()
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    searchQuery = newText?.trim() ?: ""
                    performSearch()
                    return true
                }
            })
            
            // 设置搜索框为展开状态
            isIconified = false
            requestFocus()
        }
    }
    
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task -> handleTaskClick(task) },
            onCheckboxClick = { taskId -> 
                viewModel.toggleTaskCompleted(taskId)
            },
            onStarClick = { taskId -> 
                viewModel.toggleTaskStarred(taskId)
            },
            onDeleteClick = { taskId -> handleDeleteClick(taskId) },
            getCategoryName = { categoryId -> categoryCache[categoryId] }
        )
        
        binding.recyclerSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
            itemAnimator = DefaultItemAnimator()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.tasks.collect { allTasks ->
                performSearch(allTasks)
            }
        }
        
        // 观察分类数据，用于更新缓存
        lifecycleScope.launch {
            categoryViewModel.categories.collect { categories ->
                categoryCache.clear()
                categories.forEach { category ->
                    categoryCache[category.id] = category.name
                }
                // 更新adapter以反映分类名称的变化
                taskAdapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun performSearch(tasks: List<Task> = emptyList()) {
        if (searchQuery.isEmpty()) {
            // 如果搜索为空，显示空状态
            binding.recyclerSearchResults.visibility = View.GONE
            binding.layoutEmptySearch.root.visibility = View.VISIBLE
            binding.layoutEmptySearch.textEmptyTitle.text = getString(R.string.search_empty_title)
            binding.layoutEmptySearch.textEmptySubtitle.text = getString(R.string.search_empty_subtitle)
            return
        }
        
        val filtered = tasks.filter { task ->
            !task.deleted && (
                task.description.contains(searchQuery, ignoreCase = true)
            )
        }
        
        if (filtered.isEmpty()) {
            // 搜索结果为空
            binding.recyclerSearchResults.visibility = View.GONE
            binding.layoutEmptySearch.root.visibility = View.VISIBLE
            binding.layoutEmptySearch.textEmptyTitle.text = getString(R.string.no_results_found)
            binding.layoutEmptySearch.textEmptySubtitle.text = getString(R.string.try_different_search)
        } else {
            // 显示搜索结果
            binding.recyclerSearchResults.visibility = View.VISIBLE
            binding.layoutEmptySearch.root.visibility = View.GONE
            taskAdapter.submitList(filtered)
        }
    }
    
    // 自己处理任务点击，不再依赖TaskListFragment的私有方法
    private fun handleTaskClick(task: Task) {
        // 使用简化的编辑方式，而不是复杂的对话框
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_task))
            .setMessage("是否要编辑此任务: ${task.description}")
            .setPositiveButton("编辑") { _, _ ->
                // 这里可以启动详细的编辑界面或使用更简单的编辑方式
                // 暂时使用简单的文本编辑
                showSimpleEditDialog(task)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showSimpleEditDialog(task: Task) {
        val input = EditText(requireContext()).apply {
            setText(task.description)
            setSingleLine()
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑任务")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newDescription = input.text.toString().trim()
                if (newDescription.isNotBlank()) {
                    viewModel.updateTask(
                        task.copy(description = newDescription)
                    )
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun handleDeleteClick(taskId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_task))
            .setMessage(getString(R.string.delete_task_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTask(taskId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun getRepeatText(repeatInterval: RepeatInterval): String {
        return when (repeatInterval) {
            RepeatInterval.NONE -> getString(R.string.task_repeat_none)
            RepeatInterval.DAILY -> getString(R.string.task_repeat_daily)
            RepeatInterval.WEEKLY -> getString(R.string.task_repeat_weekly)
            RepeatInterval.MONTHLY -> getString(R.string.task_repeat_monthly)
            RepeatInterval.YEARLY -> getString(R.string.task_repeat_yearly)
        }
    }
    
    private fun showRepeatOptionsDialog(onRepeatSelected: (RepeatInterval) -> Unit) {
        val options = arrayOf(
            getString(R.string.task_repeat_none),
            getString(R.string.task_repeat_daily),
            getString(R.string.task_repeat_weekly),
            getString(R.string.task_repeat_monthly),
            getString(R.string.task_repeat_yearly)
        )
        
        val intervals = arrayOf(
            RepeatInterval.NONE,
            RepeatInterval.DAILY,
            RepeatInterval.WEEKLY,
            RepeatInterval.MONTHLY,
            RepeatInterval.YEARLY
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_repeat_option))
            .setItems(options) { dialog, which ->
                onRepeatSelected(intervals[which])
                dialog.dismiss()
            }
            .show()
    }
      private fun showAdvancedRepeatDialog(onRepeatSettingsSelected: (RepeatSettings?) -> Unit) {
        // 简化版本，暂时只是创建一个空的RepeatSettings
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("高级重复设置")
            .setMessage("高级重复设置功能正在开发中")
            .setPositiveButton("确定") { _, _ ->
                // 创建一个基本的 RepeatSettings 对象
                val settings = RepeatSettings(
                    weeklyDays = emptySet(),
                    interval = 1,
                    endDate = null,
                    maxOccurrences = null
                )
                onRepeatSettingsSelected(settings)
            }
            .setNegativeButton("取消") { _, _ ->
                onRepeatSettingsSelected(null)
            }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = SearchFragment()
    }
}
