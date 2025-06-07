package takagicom.todo.jodo.ui

import takagicom.todo.jodo.model.TaskFilter

class CompletedTasksFragment : TaskListFragment() {
    override fun applyFilterToViewModel() {
        viewModel.applyFilter(TaskFilter.COMPLETED)
    }
    
    companion object {
        fun newInstance() = TaskListFragment.newInstance(TaskFilter.COMPLETED)
    }
}
