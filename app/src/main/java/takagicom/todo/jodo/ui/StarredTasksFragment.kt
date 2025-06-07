package takagicom.todo.jodo.ui

import takagicom.todo.jodo.model.TaskFilter

class StarredTasksFragment : TaskListFragment() {
    override fun applyFilterToViewModel() {
        viewModel.applyFilter(TaskFilter.STARRED)
    }
    
    companion object {
        fun newInstance() = TaskListFragment.newInstance(TaskFilter.STARRED)
    }
}
