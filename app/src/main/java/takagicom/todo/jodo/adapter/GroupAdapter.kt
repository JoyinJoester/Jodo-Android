package takagicom.todo.jodo.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Category

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit = {},
    private val onCategoryLongClick: (Category) -> Unit = {},
    private val showTaskCount: Boolean = false,
    private val taskCountProvider: ((Long) -> Int)? = null,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val nameView: TextView = itemView.findViewById(R.id.categoryName)
        private val descriptionView: TextView = itemView.findViewById(R.id.categoryDescription)
        private val taskCountView: TextView = itemView.findViewById(R.id.taskCount)
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)

        fun bind(category: Category) {
            nameView.text = category.name
            descriptionView.text = category.description
            colorIndicator.setBackgroundColor(category.color)
            
            // 设置图标（这里可以根据icon字符串设置对应的图标资源）
            setIconResource(category.icon)
            
            // 显示任务数量
            if (showTaskCount && taskCountProvider != null) {
                val count = taskCountProvider.invoke(category.id)
                taskCountView.text = count.toString()
                taskCountView.visibility = View.VISIBLE
            } else {
                taskCountView.visibility = View.GONE
            }
            
            // 如果描述为空，隐藏描述视图
            if (category.description.isBlank()) {
                descriptionView.visibility = View.GONE
            } else {
                descriptionView.visibility = View.VISIBLE
            }
              itemView.setOnClickListener {
                onCategoryClick(category)
            }
            
            itemView.setOnLongClickListener {
                onCategoryLongClick(category)
                // 如果提供了拖动回调，启动拖动
                onStartDrag?.invoke(this)
                true
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
                "ic_folder_open" -> R.drawable.ic_folder_open_24
                else -> R.drawable.ic_folder_24
            }
            iconView.setImageResource(iconResId)
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
