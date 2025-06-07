package takagicom.todo.jodo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.databinding.FragmentHomeBinding
import takagicom.todo.jodo.viewmodel.TaskViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        // 获取 TaskViewModel 实例
        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]
        
        setupUI()
        observeData()
        
        return binding.root
    }
    
    override fun onResume() {
        super.onResume()
        // 确保标题显示为"主页"
        requireActivity().title = "主页"
    }
    
    private fun setupUI() {
        // 设置卡片点击监听器
        binding.cardTodayTasks.setOnClickListener {
            findNavController().navigate(R.id.nav_today_tasks)
        }
        
        binding.cardStarredTasks.setOnClickListener {
            findNavController().navigate(R.id.nav_starred_tasks)
        }
        
        binding.cardCategoryManagement.setOnClickListener {
            findNavController().navigate(R.id.nav_category_management)
        }
        
        binding.cardStatistics.setOnClickListener {
            findNavController().navigate(R.id.nav_statistics)
        }
    }
    
    private fun observeData() {
        // 观察任务数据变化
        lifecycleScope.launch {
            taskViewModel.tasks.collect { tasks ->
                // 检查Fragment是否还存活
                if (_binding == null || !isAdded) return@collect
                
                val totalTasks = tasks.size
                val completedTasks = tasks.count { it.completed }
                val pendingTasks = totalTasks - completedTasks
                
                // 更新统计数据
                binding.textTotalTasks.text = totalTasks.toString()
                binding.textCompletedTasks.text = completedTasks.toString()
                binding.textPendingTasks.text = pendingTasks.toString()
                
                // 更新今日任务数量
                val todayTasks = tasks.filter { task ->
                    task.due_date?.toLocalDate()?.isEqual(java.time.LocalDate.now()) == true
                }
                val todayTasksText = if (todayTasks.isEmpty()) {
                    "今天没有任务"
                } else {
                    "今天有 ${todayTasks.size} 项任务"
                }
                binding.textTodayTasksCount.text = todayTasksText
                
                // 更新星标任务数量
                val starredTasks = tasks.filter { it.starred }
                val starredTasksText = if (starredTasks.isEmpty()) {
                    "暂无星标任务"
                } else {
                    "有 ${starredTasks.size} 项星标任务"
                }
                binding.textStarredTasksCount.text = starredTasksText
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
