package takagicom.todo.jodo.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.adapter.TaskAdapter
import takagicom.todo.jodo.databinding.DialogAddTaskBinding
import takagicom.todo.jodo.databinding.DialogRepeatAdvancedBinding
import takagicom.todo.jodo.databinding.DialogRepeatOptionsBinding
import takagicom.todo.jodo.databinding.FragmentTaskListBinding
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.model.RepeatInterval
import takagicom.todo.jodo.model.RepeatSettings
import takagicom.todo.jodo.model.Task
import takagicom.todo.jodo.model.TaskFilter
import takagicom.todo.jodo.viewmodel.TaskViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

open class TaskListFragment : Fragment() {
    
    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    
    // 使用Activity级别的共享ViewModel，确保所有Fragment使用同一个实例
    protected val viewModel: TaskViewModel by viewModels(
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
    
    // 分类缓存
    private val categoryCache = mutableMapOf<Long, String>()
    
    private var filterType = TaskFilter.ALL
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val filterTypeOrdinal = it.getInt(ARG_FILTER_TYPE, TaskFilter.ALL.ordinal)
            filterType = TaskFilter.values()[filterTypeOrdinal]
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
          // 不再在Fragment级别应用过滤器，完全由MainActivity控制
        // 这确保了过滤器状态的一致性
    }
    
    override fun onResume() {
        super.onResume()
        // 不在resume时重新应用过滤器，让MainActivity控制过滤状态
    }
      // 新增方法，可被子类覆盖
    protected open fun applyFilterToViewModel() {        // 完全禁用Fragment级别的过滤器应用，让MainActivity完全控制
        // 这避免了Fragment与MainActivity之间的过滤器冲突
    }
      private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { showEditTaskDialog(it) },
            onCheckboxClick = { taskId ->
                viewModel.toggleTaskCompleted(taskId)
            },
            onStarClick = { taskId ->
                viewModel.toggleTaskStarred(taskId)
            },
            onDeleteClick = { taskId -> showDeleteConfirmationDialog(taskId) },
            getCategoryName = { categoryId -> categoryCache[categoryId] }
        )
        
        binding.recyclerTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            
            // 优化列表动画 - 修复错误，删除不存在的引用
            val animator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
                supportsChangeAnimations = false // 直接设置属性而不是作为方法调用
            }
            itemAnimator = animator
            
            // 提高滚动性能
            setHasFixedSize(true)
        }
    }
    
    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }
      private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredTasks.collect { tasks ->
                taskAdapter.submitList(tasks)
                updateEmptyStateVisibility(tasks)
            }
        }
        
        // 观察分类数据，用于更新缓存
        viewLifecycleOwner.lifecycleScope.launch {
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
    
    private fun updateEmptyStateVisibility(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            // 修复visibility引用问题
            binding.layoutEmpty.root.visibility = View.VISIBLE
            binding.recyclerTasks.visibility = View.GONE
        } else {
            // 修复visibility引用问题
            binding.layoutEmpty.root.visibility = View.GONE
            binding.recyclerTasks.visibility = View.VISIBLE
        }
    }
    
    private fun showDatePicker(initialDateTime: LocalDateTime? = null, onDateSelected: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        
        // 如果有初始日期，则使用它
        initialDateTime?.let {
            calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
        }
        
        try {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    onDateSelected(year, month, dayOfMonth)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        } catch (e: Exception) {
            // 处理可能的异常
            e.printStackTrace()
            // 可能需要显示错误消息给用户
        }
    }
    
    private fun showDateTimePicker(initialDateTime: LocalDateTime, onDateTimeSelected: (LocalDateTime) -> Unit) {
        // 先显示日期选择器
        val calendar = Calendar.getInstance()
        calendar.set(initialDateTime.year, initialDateTime.monthValue - 1, initialDateTime.dayOfMonth)
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // 然后显示时间选择器
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        val selectedDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                        onDateTimeSelected(selectedDateTime)
                    },
                    initialDateTime.hour,
                    initialDateTime.minute,
                    true // 24小时制
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        var selectedDueDate: LocalDateTime? = null
        var selectedReminderTime: LocalDateTime? = null
        var selectedRepeatInterval = RepeatInterval.NONE
        var selectedRepeatSettings: RepeatSettings? = null
        var selectedCategory: Category? = null
        
        // 设置分类选择按钮
        dialogBinding.buttonCategory.setOnClickListener {
            CategorySelectionDialog.show(
                parentFragmentManager,
                selectedCategory?.id
            ) { category ->
                selectedCategory = category
                updateCategoryButton(dialogBinding, category)
            }
        }
        
        // 设置日期选择器
        dialogBinding.layoutDueDate.setEndIconOnClickListener {
            showDatePicker { year, month, day ->
                val dueDate = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                selectedDueDate = dueDate
                dialogBinding.editDueDate.setText(
                    dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        }
        
        dialogBinding.editDueDate.setOnClickListener {
            showDatePicker { year, month, day ->
                val dueDate = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                selectedDueDate = dueDate
                dialogBinding.editDueDate.setText(
                    dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        }
        
        // 设置提醒复选框
        dialogBinding.checkboxReminder.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            // 如果勾选了提醒且有截止日期，则默认设置提醒时间为截止日期前1小时
            if (isChecked && selectedDueDate != null && dialogBinding.editReminderTime.text.toString().isEmpty()) {
                val reminderTime = selectedDueDate!!.minusHours(1)
                selectedReminderTime = reminderTime
                dialogBinding.editReminderTime.setText(
                    reminderTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        }
        
        // 设置提醒时间选择器
        dialogBinding.layoutReminderTime.setEndIconOnClickListener {
            if (selectedDueDate == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("请先选择截止日期")
                    .setPositiveButton("确定", null)
                    .show()
                return@setEndIconOnClickListener
            }
            
            showDateTimePicker(selectedReminderTime ?: selectedDueDate!!.minusHours(1)) { dateTime ->
                selectedReminderTime = dateTime
                dialogBinding.editReminderTime.setText(
                    dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        }
        
        dialogBinding.editReminderTime.setOnClickListener {
            if (selectedDueDate == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("请先选择截止日期")
                    .setPositiveButton("确定", null)
                    .show()
                return@setOnClickListener
            }
            
            showDateTimePicker(selectedReminderTime ?: selectedDueDate!!.minusHours(1)) { dateTime ->
                selectedReminderTime = dateTime
                dialogBinding.editReminderTime.setText(
                    dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        }
          // 设置重复按钮
        dialogBinding.buttonRepeat.setOnClickListener {
            showRepeatOptionsDialog(selectedRepeatInterval) { repeatInterval ->
                selectedRepeatInterval = repeatInterval
                dialogBinding.buttonRepeat.text = getRepeatText(repeatInterval)
                
                // 显示或隐藏高级按钮
                dialogBinding.buttonRepeatAdvanced.visibility = 
                    if (repeatInterval != RepeatInterval.NONE) View.VISIBLE else View.GONE
            }
        }
        
        // 设置高级重复按钮
        dialogBinding.buttonRepeatAdvanced.setOnClickListener {            showAdvancedRepeatDialog { repeatSettings ->
                selectedRepeatSettings = repeatSettings
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null) // 先设为null，稍后处理
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val description = dialogBinding.editTaskDescription.text.toString().trim()
                if (description.isBlank()) {
                    dialogBinding.layoutTaskDescription.error = getString(R.string.task_description_required)
                    return@setOnClickListener
                }

                                // 检查如果勾选了提醒，必须有提醒时间
                val isReminderChecked = dialogBinding.checkboxReminder.isChecked
                if (isReminderChecked && selectedReminderTime == null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage("请设置提醒时间")
                        .setPositiveButton("确定", null)
                        .show()
                    return@setOnClickListener
                }
                
                viewModel.addTask(
                    description = description,
                    dueDate = selectedDueDate,
                    starred = dialogBinding.checkboxStarred.isChecked,
                    reminder = isReminderChecked,
                    reminderTime = selectedReminderTime,
                    repeatInterval = selectedRepeatInterval,
                    repeatSettings = selectedRepeatSettings,
                    categoryId = selectedCategory?.id ?: 0L
                )
                dialog.dismiss()
            }
        }
          dialog.show()
    }
    
    private fun showEditTaskDialog(task: Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        
        var selectedDueDate: LocalDateTime? = task.due_date
        var selectedReminderTime: LocalDateTime? = task.reminder_time
        var selectedRepeatInterval = task.repeat
        var selectedRepeatSettings: RepeatSettings? = task.repeatSettings
        var selectedCategory: Category? = null
        
        // 初始化分类选择
        if (task.categoryId != 0L) {
            // 需要从CategoryViewModel获取分类信息
            viewLifecycleOwner.lifecycleScope.launch {
                val categoryViewModel = androidx.lifecycle.ViewModelProvider(
                    requireActivity(),
                    androidx.lifecycle.SavedStateViewModelFactory(requireActivity().application, requireActivity())
                )[takagicom.todo.jodo.viewmodel.CategoryViewModel::class.java]
                
                selectedCategory = categoryViewModel.getCategoryById(task.categoryId)
                updateCategoryButton(dialogBinding, selectedCategory)
            }
        } else {
            updateCategoryButton(dialogBinding, null)
        }
        
        // 设置分类选择按钮
        dialogBinding.buttonCategory.setOnClickListener {
            CategorySelectionDialog.show(
                parentFragmentManager,
                selectedCategory?.id
            ) { category ->
                selectedCategory = category
                updateCategoryButton(dialogBinding, category)
            }
        }
        
        // 设置对话框标题
        dialogBinding.textDialogTitle.text = getString(R.string.edit_task)
        
        // 初始化对话框内容
        dialogBinding.editTaskDescription.setText(task.description)
        dialogBinding.checkboxStarred.isChecked = task.starred
        dialogBinding.checkboxReminder.isChecked = task.reminder
        
        if (task.due_date != null) {
            dialogBinding.editDueDate.setText(
                task.due_date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )
        }
        
        // 初始化提醒时间显示
        if (task.reminder) {
            dialogBinding.layoutReminderTime.visibility = View.VISIBLE
            if (task.reminder_time != null) {
                dialogBinding.editReminderTime.setText(
                    task.reminder_time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        } else {
            dialogBinding.layoutReminderTime.visibility = View.GONE
        }
        
        // 初始化重复选项显示
        dialogBinding.buttonRepeat.text = getRepeatText(task.repeat)
        
        // 设置日期选择器
        dialogBinding.layoutDueDate.setEndIconOnClickListener {
            showDatePicker { year, month, day ->
                val dueDate = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                selectedDueDate = dueDate
                dialogBinding.editDueDate.setText(
                    dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        }
        
        dialogBinding.editDueDate.setOnClickListener {
            showDatePicker { year, month, day ->
                val dueDate = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                selectedDueDate = dueDate
                dialogBinding.editDueDate.setText(
                    dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        }
        
        // 设置提醒复选框
        dialogBinding.checkboxReminder.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            // 如果勾选了提醒且有截止日期，则默认设置提醒时间为截止日期前1小时
            if (isChecked && selectedDueDate != null && dialogBinding.editReminderTime.text.toString().isEmpty()) {
                val reminderTime = selectedDueDate!!.minusHours(1)
                selectedReminderTime = reminderTime
                dialogBinding.editReminderTime.setText(
                    reminderTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        }
        
        // 设置提醒时间选择器
        dialogBinding.layoutReminderTime.setEndIconOnClickListener {
            if (selectedDueDate == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("请先选择截止日期")
                    .setPositiveButton("确定", null)
                    .show()
                return@setEndIconOnClickListener
            }
            
            showDateTimePicker(selectedReminderTime ?: selectedDueDate!!.minusHours(1)) { dateTime ->
                selectedReminderTime = dateTime
                dialogBinding.editReminderTime.setText(
                    dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        }
        
        dialogBinding.editReminderTime.setOnClickListener {
            if (selectedDueDate == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("请先选择截止日期")
                    .setPositiveButton("确定", null)
                    .show()
                return@setOnClickListener
            }
            
            showDateTimePicker(selectedReminderTime ?: selectedDueDate!!.minusHours(1)) { dateTime ->
                selectedReminderTime = dateTime
                dialogBinding.editReminderTime.setText(
                    dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
            }
        }
          // 设置重复按钮
        dialogBinding.buttonRepeat.setOnClickListener {
            showRepeatOptionsDialog(selectedRepeatInterval) { repeatInterval ->
                selectedRepeatInterval = repeatInterval
                dialogBinding.buttonRepeat.text = getRepeatText(repeatInterval)
                
                // 显示或隐藏高级按钮
                dialogBinding.buttonRepeatAdvanced.visibility = 
                    if (repeatInterval != RepeatInterval.NONE) View.VISIBLE else View.GONE
            }
        }
        
        // 设置高级重复按钮
        dialogBinding.buttonRepeatAdvanced.setOnClickListener {            showAdvancedRepeatDialog { repeatSettings ->
                selectedRepeatSettings = repeatSettings
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null) // 先设为null，稍后处理
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val description = dialogBinding.editTaskDescription.text.toString().trim()
                if (description.isBlank()) {
                    dialogBinding.layoutTaskDescription.error = getString(R.string.task_description_required)
                    return@setOnClickListener
                }
                
                                // 检查如果勾选了提醒，必须有提醒时间
                val isReminderChecked = dialogBinding.checkboxReminder.isChecked
                if (isReminderChecked && selectedReminderTime == null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage("请设置提醒时间")
                        .setPositiveButton("确定", null)
                        .show()
                    return@setOnClickListener
                }
                
                // 这里使用了所有变量：selectedDueDate, selectedReminderTime, selectedRepeatInterval, selectedRepeatSettings, selectedCategory
                viewModel.updateTask(
                    task.copy(
                        description = description,
                        due_date = selectedDueDate,
                        starred = dialogBinding.checkboxStarred.isChecked,
                        reminder = isReminderChecked,
                        reminder_time = if (isReminderChecked) selectedReminderTime else null,
                        repeat = selectedRepeatInterval,
                        repeatSettings = selectedRepeatSettings,
                        categoryId = selectedCategory?.id ?: 0L
                    )
                )
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showRepeatOptionsDialog(currentRepeat: RepeatInterval, onRepeatSelected: (RepeatInterval) -> Unit) {
        val dialogBinding = DialogRepeatOptionsBinding.inflate(layoutInflater)
        
        // 设置当前选中的重复选项
        when (currentRepeat) {
            RepeatInterval.NONE -> dialogBinding.radioNone.isChecked = true
            RepeatInterval.DAILY -> dialogBinding.radioDaily.isChecked = true
            RepeatInterval.WEEKLY -> dialogBinding.radioWeekly.isChecked = true
            RepeatInterval.MONTHLY -> dialogBinding.radioMonthly.isChecked = true
            RepeatInterval.YEARLY -> dialogBinding.radioYearly.isChecked = true
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val selectedInterval = when {
                    dialogBinding.radioNone.isChecked -> RepeatInterval.NONE
                    dialogBinding.radioDaily.isChecked -> RepeatInterval.DAILY
                    dialogBinding.radioWeekly.isChecked -> RepeatInterval.WEEKLY
                    dialogBinding.radioMonthly.isChecked -> RepeatInterval.MONTHLY
                    dialogBinding.radioYearly.isChecked -> RepeatInterval.YEARLY
                    else -> RepeatInterval.NONE
                }
                onRepeatSelected(selectedInterval)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        
        dialog.show()
    }
      private fun showAdvancedRepeatDialog(onRepeatSettingsSelected: (RepeatSettings) -> Unit) {
        val dialogBinding = DialogRepeatAdvancedBinding.inflate(layoutInflater)
        
        var selectedRepeatType = "weekly"
        var selectedRepeatSettings = RepeatSettings()
        
        // 初始选择周重复
        dialogBinding.radioWeekly.isChecked = true
        dialogBinding.cardWeeklySettings.visibility = View.VISIBLE
        dialogBinding.textIntervalUnit.text = "周"
        
        // 重复类型选择
        dialogBinding.radioGroupRepeatType.setOnCheckedChangeListener { _, checkedId ->
            // 隐藏所有设置卡片
            dialogBinding.cardWeeklySettings.visibility = View.GONE
            dialogBinding.cardMonthlySettings.visibility = View.GONE
            dialogBinding.cardYearlySettings.visibility = View.GONE
            
            when (checkedId) {
                R.id.radio_weekly -> {
                    selectedRepeatType = "weekly"
                    dialogBinding.cardWeeklySettings.visibility = View.VISIBLE
                    dialogBinding.textIntervalUnit.text = "周"
                }
                R.id.radio_monthly -> {
                    selectedRepeatType = "monthly"
                    dialogBinding.cardMonthlySettings.visibility = View.VISIBLE
                    dialogBinding.textIntervalUnit.text = "月"
                }
                R.id.radio_yearly -> {
                    selectedRepeatType = "yearly"
                    dialogBinding.cardYearlySettings.visibility = View.VISIBLE
                    dialogBinding.textIntervalUnit.text = "年"
                }
            }
        }
        
        // 结束条件选择
        dialogBinding.radioGroupEndCondition.setOnCheckedChangeListener { _, checkedId ->
            dialogBinding.layoutEndDate.visibility = View.GONE
            dialogBinding.layoutMaxOccurrences.visibility = View.GONE
            
            when (checkedId) {
                R.id.radio_end_date -> {
                    dialogBinding.layoutEndDate.visibility = View.VISIBLE
                }
                R.id.radio_max_occurrences -> {
                    dialogBinding.layoutMaxOccurrences.visibility = View.VISIBLE
                }
            }
        }
        
        // 结束日期选择
        dialogBinding.layoutEndDate.setEndIconOnClickListener {
            showDatePicker { year, month, day ->
                val endDate = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                dialogBinding.editEndDate.setText(
                    endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        }
        
        dialogBinding.editEndDate.setOnClickListener {
            showDatePicker { year, month, day ->
                val endDate = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                dialogBinding.editEndDate.setText(
                    endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            }
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                // 收集设置
                val interval = dialogBinding.editInterval.text.toString().toIntOrNull() ?: 1
                
                val endDate = if (dialogBinding.radioEndDate.isChecked && 
                    dialogBinding.editEndDate.text.toString().isNotEmpty()) {
                    try {
                        LocalDateTime.parse(
                            dialogBinding.editEndDate.text.toString() + " 23:59:59",
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        )
                    } catch (e: Exception) { null }
                } else null
                
                val maxOccurrences = if (dialogBinding.radioMaxOccurrences.isChecked &&
                    dialogBinding.editMaxOccurrences.text.toString().isNotEmpty()) {
                    dialogBinding.editMaxOccurrences.text.toString().toIntOrNull()
                } else null
                
                when (selectedRepeatType) {
                    "weekly" -> {
                        val weeklyDays = mutableSetOf<Int>()
                        if (dialogBinding.checkboxMonday.isChecked) weeklyDays.add(1)
                        if (dialogBinding.checkboxTuesday.isChecked) weeklyDays.add(2)
                        if (dialogBinding.checkboxWednesday.isChecked) weeklyDays.add(3)
                        if (dialogBinding.checkboxThursday.isChecked) weeklyDays.add(4)
                        if (dialogBinding.checkboxFriday.isChecked) weeklyDays.add(5)
                        if (dialogBinding.checkboxSaturday.isChecked) weeklyDays.add(6)
                        if (dialogBinding.checkboxSunday.isChecked) weeklyDays.add(7)
                        
                        selectedRepeatSettings = RepeatSettings(
                            weeklyDays = weeklyDays,
                            interval = interval,
                            endDate = endDate,
                            maxOccurrences = maxOccurrences
                        )
                    }
                    "monthly" -> {
                        val monthlyDay = dialogBinding.editMonthlyDay.text.toString().toIntOrNull()
                        selectedRepeatSettings = RepeatSettings(
                            monthlyDay = monthlyDay,
                            interval = interval,
                            endDate = endDate,
                            maxOccurrences = maxOccurrences
                        )
                    }
                    "yearly" -> {
                        val yearlyMonth = dialogBinding.editYearlyMonth.text.toString().toIntOrNull()
                        val yearlyDay = dialogBinding.editYearlyDay.text.toString().toIntOrNull()
                        selectedRepeatSettings = RepeatSettings(
                            yearlyMonth = yearlyMonth,
                            yearlyDay = yearlyDay,
                            interval = interval,
                            endDate = endDate,
                            maxOccurrences = maxOccurrences
                        )
                    }
                }
                
                onRepeatSettingsSelected(selectedRepeatSettings)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            
        dialog.show()
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
    
    // 美化删除确认对话框
    private fun showDeleteConfirmationDialog(taskId: Long) {
        // 获取任务信息用于显示在对话框中
        val task = viewModel.getTaskById(taskId)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null)
        val taskDescription = dialogView.findViewById<TextView>(R.id.text_task_description)
        
        task?.let {
            // 最多显示50个字符，超出部分用省略号替代
            val truncatedDesc = if (it.description.length > 50) 
                "${it.description.substring(0, 47)}..." 
            else 
                it.description
            taskDescription.text = truncatedDesc
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                // 创建一个淡出动画效果
                val taskPosition = taskAdapter.currentList.indexOfFirst { it.id == taskId }
                if (taskPosition != -1) {
                    val taskView = binding.recyclerTasks.findViewHolderForAdapterPosition(taskPosition)?.itemView
                    taskView?.animate()
                        ?.alpha(0f)
                        ?.translationX(taskView.width.toFloat())
                        ?.setDuration(300)
                        ?.withEndAction {
                            viewModel.deleteTask(taskId)
                        }
                        ?.start()
                } else {
                    viewModel.deleteTask(taskId)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun updateCategoryButton(dialogBinding: DialogAddTaskBinding, category: Category?) {
        if (category == null || category.id == 0L) {
            dialogBinding.buttonCategory.text = getString(R.string.task_category_none)
            dialogBinding.buttonCategory.setIconResource(R.drawable.ic_folder_24)
        } else {
            dialogBinding.buttonCategory.text = category.name
            val iconResId = when (category.icon) {
                "ic_work" -> R.drawable.ic_work_24
                "ic_person" -> R.drawable.ic_person_24
                "ic_school" -> R.drawable.ic_school_24
                "ic_shopping_cart" -> R.drawable.ic_shopping_cart_24
                "ic_favorite" -> R.drawable.ic_favorite_24
                "ic_home" -> R.drawable.ic_home_24
                "ic_sports" -> R.drawable.ic_sports_24
                "ic_restaurant" -> R.drawable.ic_restaurant_24
                "ic_local_hospital" -> R.drawable.ic_local_hospital_24
                "ic_directions_car" -> R.drawable.ic_directions_car_24
                "ic_flight" -> R.drawable.ic_flight_24
                "ic_music_note" -> R.drawable.ic_music_note_24
                "ic_camera" -> R.drawable.ic_camera_alt_24
                "ic_pets" -> R.drawable.ic_pets_24
                "ic_eco" -> R.drawable.ic_eco_24
                "ic_build" -> R.drawable.ic_build_24
                else -> R.drawable.ic_folder_24
            }
            dialogBinding.buttonCategory.setIconResource(iconResId)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_FILTER_TYPE = "filter_type"
        
        fun newInstance(filterType: TaskFilter = TaskFilter.ALL): TaskListFragment {
            return TaskListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_FILTER_TYPE, filterType.ordinal)
                }
            }
        }
    }
}
