package takagicom.todo.jodo.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Task
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.content.res.ColorStateList

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onCheckboxClick: (Long) -> Unit,
    private val onStarClick: (Long) -> Unit,
    private val onDeleteClick: (Long) -> Unit,
    private val getCategoryName: (Long) -> String? = { null }
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDescription: TextView = itemView.findViewById(R.id.text_description)
        private val textDueDate: TextView = itemView.findViewById(R.id.text_due_date)
        private val textCategory: TextView = itemView.findViewById(R.id.text_category)
        private val textRepeat: TextView = itemView.findViewById(R.id.text_repeat)
        private val textReminder: TextView = itemView.findViewById(R.id.text_reminder)
        private val textPriority: TextView = itemView.findViewById(R.id.text_priority)
        private val checkboxCompleted: CheckBox = itemView.findViewById(R.id.checkbox_completed)
        private val buttonStar: ImageButton = itemView.findViewById(R.id.button_star)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.button_delete)
        private val priorityIndicator: View? = itemView.findViewById(R.id.priority_indicator)
        private val cardView: MaterialCardView = itemView as MaterialCardView

        fun bind(task: Task) {
            // 首先移除所有监听器，防止重用引起的问题
            checkboxCompleted.setOnCheckedChangeListener(null)
            buttonStar.setOnClickListener(null)
            buttonDelete.setOnClickListener(null)
            itemView.setOnClickListener(null)
            
            // 设置任务描述
            textDescription.text = task.description
            
            // 设置完成状态样式
            updateTaskCompletedAppearance(task.completed)
              // 设置截止日期
            if (task.due_date != null) {
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                textDueDate.text = task.due_date.format(formatter)
                textDueDate.visibility = View.VISIBLE
                
                if (task.completed) {
                    textDueDate.alpha = 0.7f
                } else {
                    textDueDate.alpha = 1.0f
                }
            } else {
                textDueDate.visibility = View.GONE
            }

            // 设置分类信息
            if (task.categoryId != 0L) {
                val categoryName = getCategoryName(task.categoryId)
                if (!categoryName.isNullOrBlank()) {
                    textCategory.text = categoryName
                    textCategory.visibility = View.VISIBLE
                    
                    if (task.completed) {
                        textCategory.alpha = 0.7f
                    } else {
                        textCategory.alpha = 0.8f
                    }
                } else {
                    textCategory.visibility = View.GONE
                }            } else {
                textCategory.visibility = View.GONE
            }            // 设置重复设置信息
            if (task.repeat != takagicom.todo.jodo.model.RepeatInterval.NONE) {
                val repeatText = getRepeatText(task.repeat, task.repeatSettings)
                textRepeat.text = repeatText
                textRepeat.visibility = View.VISIBLE
                
                if (task.completed) {
                    textRepeat.alpha = 0.7f
                } else {
                    textRepeat.alpha = 0.8f
                }
            } else {
                textRepeat.visibility = View.GONE
            }            // 设置提醒信息
            if (task.reminder_time != null) {
                textReminder.text = "提醒"
                textReminder.visibility = View.VISIBLE
                
                if (task.completed) {
                    textReminder.alpha = 0.7f
                } else {
                    textReminder.alpha = 0.8f
                }
            } else {
                textReminder.visibility = View.GONE
            }            // 设置优先级信息 - 移除重要标识显示
            textPriority.visibility = View.GONE// 设置复选框状态 - 防止闪退的关键修复
            checkboxCompleted.isChecked = task.completed
              // 使用简单的点击事件替代复选框的 OnCheckedChangeListener
            checkboxCompleted.setOnClickListener {
                android.util.Log.d("TaskAdapter", "Checkbox clicked for task: ${task.description}")
                try {
                    val position = adapterPosition
                    
                    if (position >= 0 && position < itemCount) {
                        val currentTask = getItem(position)
                        
                        if (currentTask != null) {
                            android.util.Log.d("TaskAdapter", "Checkbox click - valid task found: ${currentTask.description}, id: ${currentTask.id}")
                            // 立即更新复选框UI状态，防止闪退
                            val newCompletedState = !currentTask.completed
                            checkboxCompleted.isChecked = newCompletedState
                            
                            // 使用post确保在主线程中执行数据更新
                            itemView.post {
                                try {
                                    android.util.Log.d("TaskAdapter", "Calling onCheckboxClick for task id: ${currentTask.id}")
                                    onCheckboxClick(currentTask.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("TaskAdapter", "Error in onCheckboxClick callback", e)
                                    // 如果回调失败，恢复原状态
                                    checkboxCompleted.isChecked = currentTask.completed
                                }
                            }
                        } else {
                            android.util.Log.w("TaskAdapter", "Checkbox click - currentTask is null")
                        }
                    } else {
                        android.util.Log.w("TaskAdapter", "Checkbox click - invalid position: $position, itemCount: $itemCount")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TaskAdapter", "Error in checkbox click handler", e)
                    // 防止闪退，记录错误但不中断用户体验
                    // 尝试恢复到安全状态
                    try {
                        checkboxCompleted.isChecked = task.completed
                    } catch (recoverE: Exception) {
                    }
                }
            }

            // 设置勾选框颜色为白色
            val checkboxDrawable = checkboxCompleted.buttonDrawable
            if (checkboxDrawable != null) {
                val wrappedDrawable = DrawableCompat.wrap(checkboxDrawable).mutate()
                DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(itemView.context, R.color.white))
                checkboxCompleted.buttonDrawable = wrappedDrawable
            }            // 设置星标状态和优先级指示器
            try {
                val starDrawableRes = if (task.starred) R.drawable.ic_star else R.drawable.ic_star_outline
                buttonStar.setImageResource(starDrawableRes)
                
                // 设置星标按钮的颜色
                val starColor = if (task.starred) {
                    ContextCompat.getColor(itemView.context, R.color.star_active)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.star_inactive)
                }
                buttonStar.imageTintList = ColorStateList.valueOf(starColor)
                
            } catch (e: Exception) {
                // 设置最安全的默认图标
                try {
                    buttonStar.setImageResource(R.drawable.ic_star_outline)
                    buttonStar.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.star_inactive)
                    )
                } catch (fallbackE: Exception) {
                }
            }
            
            // 显示星标任务的优先级指示器
            priorityIndicator?.let {
                if (task.starred) {
                    it.visibility = View.VISIBLE
                    it.setBackgroundColor(
                        ContextCompat.getColor(
                            itemView.context, 
                            if (task.completed) R.color.priority_starred_completed else R.color.priority_starred
                        )
                    )
                } else {
                    it.visibility = View.GONE
                }
            }            // 使用最简单的事件绑定方式
            buttonStar.setOnClickListener {
                android.util.Log.d("TaskAdapter", "Star button clicked for task: ${task.description}")
                try {
                    val position = adapterPosition
                    
                    if (position >= 0 && position < itemCount) {
                        val task = getItem(position)
                        
                        if (task != null) {
                            android.util.Log.d("TaskAdapter", "Star click - valid task found: ${task.description}, id: ${task.id}")
                            // 立即更新UI状态，防止闪退
                            val newStarredState = !task.starred
                            try {
                                val starDrawableRes = if (newStarredState) R.drawable.ic_star else R.drawable.ic_star_outline
                                buttonStar.setImageResource(starDrawableRes)
                                
                                val starColor = if (newStarredState) {
                                    ContextCompat.getColor(itemView.context, R.color.star_active)
                                } else {
                                    ContextCompat.getColor(itemView.context, R.color.star_inactive)
                                }
                                buttonStar.imageTintList = ColorStateList.valueOf(starColor)
                                
                                // 更新优先级指示器
                                priorityIndicator?.let {
                                    if (newStarredState) {
                                        it.visibility = View.VISIBLE
                                        it.setBackgroundColor(
                                            ContextCompat.getColor(
                                                itemView.context, 
                                                if (task.completed) R.color.priority_starred_completed else R.color.priority_starred
                                            )
                                        )
                                    } else {
                                        it.visibility = View.GONE
                                    }
                                }
                            } catch (uiE: Exception) {
                                android.util.Log.e("TaskAdapter", "Error updating star UI", uiE)
                            }
                            
                            // 使用post确保在主线程中执行数据更新
                            itemView.post {
                                try {
                                    android.util.Log.d("TaskAdapter", "Calling onStarClick for task id: ${task.id}")
                                    onStarClick(task.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("TaskAdapter", "Error in onStarClick callback", e)
                                }
                            }
                        } else {
                            android.util.Log.w("TaskAdapter", "Star click - task is null")
                        }
                    } else {
                        android.util.Log.w("TaskAdapter", "Star click - invalid position: $position, itemCount: $itemCount")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TaskAdapter", "Error in star click handler", e)
                    // 防止闪退，记录错误但不中断用户体验
                }
            }
            
            buttonDelete.setOnClickListener {
                try {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION && position < itemCount) {
                        val task = getItem(position)
                        if (task != null) {
                            onDeleteClick(task.id)
                        }
                    }
                } catch (e: Exception) {
                    // 防止闪退，记录错误但不中断用户体验
                }
            }

            itemView.setOnClickListener {
                try {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION && position < itemCount) {
                        val task = getItem(position)
                        if (task != null) {
                            onTaskClick(task)
                        }
                    }
                } catch (e: Exception) {
                    // 防止闪退，记录错误但不中断用户体验
                }
            }

            // 美化删除按钮
            buttonDelete.apply {
                setImageResource(R.drawable.ic_delete_beautified)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.delete_button))
                
                // 添加按下效果
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.alpha = 0.7f
                            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.alpha = 1.0f
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }
                    }
                    false
                }
            }
        }
        
        private fun updateTaskCompletedAppearance(isCompleted: Boolean) {
            if (isCompleted) {
                textDescription.paint.isStrikeThruText = true
                textDescription.alpha = 0.7f
                cardView.alpha = 0.9f
                cardView.cardElevation = 1f
            } else {
                textDescription.paint.isStrikeThruText = false
                textDescription.alpha = 1.0f
                cardView.alpha = 1.0f
                cardView.cardElevation = 4f
            }
        }

        fun runDeleteAnimation(onAnimationEnd: () -> Unit) {
            val animator = ObjectAnimator.ofFloat(itemView, "translationX", -itemView.width.toFloat())
            animator.duration = 300
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd()
                }
            })
            animator.start()
        }        fun runCompleteAnimation(onAnimationEnd: () -> Unit) {
            val scaleDown = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(itemView, "scaleX", 1f, 0.9f, 1f))
                    .with(ObjectAnimator.ofFloat(itemView, "scaleY", 1f, 0.9f, 1f))
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onAnimationEnd()
                    }
                })
            }
            scaleDown.start()
        }

        /**
         * 获取重复设置的显示文本
         */
        private fun getRepeatText(repeatInterval: takagicom.todo.jodo.model.RepeatInterval, repeatSettings: takagicom.todo.jodo.model.RepeatSettings?): String {
        val context = itemView.context
        val baseText = when (repeatInterval) {
            takagicom.todo.jodo.model.RepeatInterval.NONE -> return ""
            takagicom.todo.jodo.model.RepeatInterval.DAILY -> context.getString(R.string.task_repeat_daily)
            takagicom.todo.jodo.model.RepeatInterval.WEEKLY -> context.getString(R.string.task_repeat_weekly)
            takagicom.todo.jodo.model.RepeatInterval.MONTHLY -> context.getString(R.string.task_repeat_monthly)
            takagicom.todo.jodo.model.RepeatInterval.YEARLY -> context.getString(R.string.task_repeat_yearly)
        }
        
        // 如果有高级设置，添加详细信息
        repeatSettings?.let { settings ->
            when (repeatInterval) {
                takagicom.todo.jodo.model.RepeatInterval.WEEKLY -> {
                    if (settings.weeklyDays.isNotEmpty()) {
                        val dayNames = settings.weeklyDays.sorted().map { dayNum ->
                            when (dayNum) {
                                1 -> "周一"
                                2 -> "周二"
                                3 -> "周三"
                                4 -> "周四"
                                5 -> "周五"
                                6 -> "周六"
                                7 -> "周日"
                                else -> ""
                            }
                        }.filter { it.isNotEmpty() }
                        
                        if (dayNames.isNotEmpty()) {
                            return "$baseText (${dayNames.joinToString(", ")})"
                        }
                    }
                }
                takagicom.todo.jodo.model.RepeatInterval.MONTHLY -> {
                    settings.monthlyDay?.let { day ->
                        return "$baseText (每月${day}日)"
                    }
                }
                takagicom.todo.jodo.model.RepeatInterval.YEARLY -> {
                    if (settings.yearlyMonth != null && settings.yearlyDay != null) {
                        return "$baseText (${settings.yearlyMonth}月${settings.yearlyDay}日)"
                    }
                }
                else -> {}
            }
            
            // 添加间隔信息
            if (settings.interval > 1) {
                val intervalText = when (repeatInterval) {
                    takagicom.todo.jodo.model.RepeatInterval.DAILY -> "每${settings.interval}天"
                    takagicom.todo.jodo.model.RepeatInterval.WEEKLY -> "每${settings.interval}周"
                    takagicom.todo.jodo.model.RepeatInterval.MONTHLY -> "每${settings.interval}月"
                    takagicom.todo.jodo.model.RepeatInterval.YEARLY -> "每${settings.interval}年"
                    else -> baseText
                }
                return intervalText
            }
        }        
        return baseText
    }
    }

    object TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
