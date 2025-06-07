package takagicom.todo.jodo.ui

import takagicom.todo.jodo.model.TaskFilter

class TodayTasksFragment : TaskListFragment() {
    override fun applyFilterToViewModel() {
        viewModel.applyFilter(TaskFilter.DUE_TODAY)
    }
    
    companion object {
        fun newInstance() = TaskListFragment.newInstance(TaskFilter.DUE_TODAY)
    }
}
