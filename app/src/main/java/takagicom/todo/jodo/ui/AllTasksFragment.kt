package takagicom.todo.jodo.ui

import takagicom.todo.jodo.model.TaskFilter

class AllTasksFragment : TaskListFragment() {
    override fun applyFilterToViewModel() {        // AllTasksFragment现在由MainActivity完全控制过滤器
        // 不再强制应用ANY特定过滤器，让MainActivity的过滤器设置生效
    }
    
    companion object {
        fun newInstance() = TaskListFragment.newInstance(TaskFilter.ALL)
    }
}
