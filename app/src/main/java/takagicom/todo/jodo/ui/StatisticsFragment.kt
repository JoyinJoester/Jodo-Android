package takagicom.todo.jodo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.databinding.FragmentStatisticsBinding
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.view.SimplePieChart
import takagicom.todo.jodo.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalDateTime

class StatisticsFragment : Fragment() {    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    
    // 使用Activity级别的共享ViewModel，确保与其他Fragment同步
    private val viewModel: TaskViewModel by viewModels(
        ownerProducer = { requireActivity() }
    ) {
        SavedStateViewModelFactory(requireActivity().application, requireActivity())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }
      override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initObservers()
    }
    
    override fun onResume() {
        super.onResume()
        // 确保在Fragment恢复时重新刷新统计数据
        refreshStatistics()
    }
    
    private fun refreshStatistics() {
        // 手动触发一次数据刷新，确保统计数据是最新的
        lifecycleScope.launch {
            val currentTasks = viewModel.tasks.value
            updateStatistics(currentTasks)
        }
    }
      private fun initObservers() {
        lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                updateStatistics(tasks)
            }
        }
    }
    
    private fun updateStatistics(tasks: List<Task>) {
        // 只考虑未删除的任务
        val activeTasks = tasks.filter { !it.deleted }
        
        // 基本统计数据
        val totalTasks = activeTasks.size
        val activeTasksCount = activeTasks.count { !it.completed }
        val completedTasksCount = activeTasks.count { it.completed }
        val starredTasksCount = activeTasks.count { it.starred }
        
        // 截止日期相关统计
        val today = LocalDate.now()
        val overdueCount = activeTasks.count { 
            !it.completed && it.due_date != null && 
            it.due_date.toLocalDate().isBefore(today)
        }
        val dueTodayCount = activeTasks.count {
            !it.completed && it.due_date != null &&
            it.due_date.toLocalDate().isEqual(today) 
        }
        val dueThisWeekCount = activeTasks.count {
            !it.completed && it.due_date != null &&
            it.due_date.toLocalDate().isAfter(today) &&
            it.due_date.toLocalDate().isBefore(today.plusDays(7))
        }
        
        // 计算完成率
        val completionRate = if (totalTasks > 0) {
            (completedTasksCount.toFloat() / totalTasks) * 100
        } else 0f
        
        // 更新界面
        updateBasicStats(
            totalTasks, 
            activeTasksCount, 
            completedTasksCount, 
            starredTasksCount,
            completionRate
        )
        
        // 更新截止日期统计
        updateDeadlineStats(
            overdueCount,
            dueTodayCount,
            dueThisWeekCount
        )
        
        // 更新活跃度统计
        updateActivityStats(tasks)
        
        // 更新任务状态饼图
        updateStatusPieChart(
            activeTasksCount,
            completedTasksCount
        )
    }
    
    private fun updateBasicStats(
        totalTasks: Int,
        activeTasks: Int,
        completedTasks: Int,
        starredTasks: Int,
        completionRate: Float
    ) {
        binding.apply {
            tvTotalTaskCount.text = totalTasks.toString()
            tvActiveCount.text = activeTasks.toString()
            tvCompletedCount.text = completedTasks.toString()
            tvStarredCount.text = starredTasks.toString()
            
            progressCompletion.progress = completionRate.toInt()
            tvCompletionRate.text = getString(R.string.completion_rate_format, completionRate)
        }
    }
    
    private fun updateDeadlineStats(
        overdueCount: Int,
        dueTodayCount: Int,
        dueThisWeekCount: Int
    ) {
        binding.apply {
            tvOverdueCount.text = overdueCount.toString()
            tvDueTodayCount.text = dueTodayCount.toString()
            tvDueThisWeekCount.text = dueThisWeekCount.toString()
        }
    }
    
    private fun updateActivityStats(tasks: List<Task>) {
        // 一周内创建的任务数
        val oneWeekAgo = LocalDateTime.now().minusDays(7)
        val createdLastWeekCount = tasks.count { task: Task ->  // 明确指定参数类型
            !task.deleted && task.created_at.isAfter(oneWeekAgo)
        }
        binding.tvCreatedLastWeek.text = createdLastWeekCount.toString()
        
        // 一周内完成的任务数
        // 由于Task类中可能没有completed_at字段，使用created_at和completed状态作为近似
        val completedLastWeekCount = tasks.count { task: Task ->  // 明确指定参数类型
            task.completed && !task.deleted && task.created_at.isAfter(oneWeekAgo)
        }
        binding.tvCompletedLastWeek.text = completedLastWeekCount.toString()
    }
    
    private fun updateStatusPieChart(activeTasks: Int, completedTasks: Int) {
        val total = activeTasks + completedTasks
        if (total == 0) return
        
        val activePercentage = activeTasks * 100f / total
        val completedPercentage = completedTasks * 100f / total
        
        val slices = listOf(
            SimplePieChart.Slice(
                getString(R.string.active_tasks),
                activePercentage,
                ContextCompat.getColor(requireContext(), R.color.primary)
            ),
            SimplePieChart.Slice(
                getString(R.string.completed_tasks),
                completedPercentage,
                ContextCompat.getColor(requireContext(), R.color.accent)
            )
        )
        
        binding.pieChartStatus.setData(slices)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = StatisticsFragment()
    }
}
