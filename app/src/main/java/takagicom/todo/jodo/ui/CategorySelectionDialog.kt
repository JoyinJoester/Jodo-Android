package takagicom.todo.jodo.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.viewmodel.CategoryViewModel

class CategorySelectionDialog : DialogFragment() {

    private var selectedCategoryId: Long? = null
    private var onCategorySelected: ((Category?) -> Unit)? = null
    
    private val categoryViewModel: CategoryViewModel by viewModels {
        SavedStateViewModelFactory(requireActivity().application, requireActivity())
    }
    
    private lateinit var adapter: CategorySelectionAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_category_selection, null)
        
        setupRecyclerView(view)
        observeViewModel()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_category)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun setupRecyclerView(view: View) {
        adapter = CategorySelectionAdapter(selectedCategoryId) { category ->
            onCategorySelected?.invoke(category)
            dismiss()
        }
        
        view.findViewById<RecyclerView>(R.id.recyclerCategorySelection).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CategorySelectionDialog.adapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            categoryViewModel.categories.collect { categories ->
                val allCategories = listOf(Category.UNCATEGORIZED) + categories.sortedBy { it.orderIndex }
                adapter.submitList(allCategories)
            }
        }
    }

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            selectedCategoryId: Long? = null,
            onCategorySelected: (Category?) -> Unit
        ) {
            val dialog = CategorySelectionDialog().apply {
                this.selectedCategoryId = selectedCategoryId
                this.onCategorySelected = onCategorySelected
            }
            dialog.show(fragmentManager, "CategorySelectionDialog")
        }
    }
}

class CategorySelectionAdapter(
    private var selectedCategoryId: Long?,
    private val onCategoryClick: (Category?) -> Unit
) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

    private var categories = listOf<Category>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    fun submitList(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val selectedIndicator: ImageView = itemView.findViewById(R.id.selectedIndicator)

        fun bind(category: Category) {
            categoryName.text = category.name
            colorIndicator.setBackgroundColor(category.color)
            setIconResource(category.icon)
            
            // 显示选中状态
            val isSelected = category.id == selectedCategoryId
            selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener {
                val selectedCategory = if (category.id == 0L) null else category
                onCategoryClick(selectedCategory)
            }
        }
        
        private fun setIconResource(iconName: String) {
            val iconResId = when (iconName) {
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
            categoryIcon.setImageResource(iconResId)
        }
    }
}
