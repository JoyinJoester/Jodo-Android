package takagicom.todo.jodo.ui

import takagicom.todo.jodo.model.TaskFilter

class ActiveTasksFragment : TaskListFragment() {
    override fun applyFilterToViewModel() {
        viewModel.applyFilter(TaskFilter.ACTIVE)
    }
    
    companion object {
        fun newInstance() = TaskListFragment.newInstance(TaskFilter.ACTIVE)
    }
}
