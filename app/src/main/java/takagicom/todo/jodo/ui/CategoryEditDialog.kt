package takagicom.todo.jodo.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import takagicom.todo.jodo.R
import takagicom.todo.jodo.model.Category
import takagicom.todo.jodo.viewmodel.CategoryViewModel

class CategoryEditDialog : DialogFragment() {

    private var category: Category? = null
    private var isDefaultCategory: Boolean = false
    private var onSaveCallback: ((String, Int, String, String) -> Unit)? = null
    
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var colorRecyclerView: RecyclerView
    private lateinit var iconRecyclerView: RecyclerView
    private lateinit var selectedColorView: View
    private lateinit var selectedIconView: ImageView
    
    private var selectedColor: Int = 0xFF2196F3.toInt()
    private var selectedIcon: String = "ic_folder"
    
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var iconAdapter: IconAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_category_edit, null)
        
        initViews(view)
        setupAdapters()
        setupInitialValues()
        
        val titleText = if (category == null) "添加分类" else "编辑分类"
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleText)
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                saveCategory()
            }
            .setNegativeButton("取消", null)
            .create()
    }

    private fun initViews(view: View) {
        nameEditText = view.findViewById(R.id.editCategoryName)
        descriptionEditText = view.findViewById(R.id.editCategoryDescription)
        colorRecyclerView = view.findViewById(R.id.colorRecyclerView)
        iconRecyclerView = view.findViewById(R.id.iconRecyclerView)
        selectedColorView = view.findViewById(R.id.selectedColorIndicator)
        selectedIconView = view.findViewById(R.id.selectedIconView)
        
        // 如果是默认分类，禁用名称编辑
        if (isDefaultCategory) {
            nameEditText.isEnabled = false
            view.findViewById<TextView>(R.id.labelCategoryName).text = "分类名称（系统分类不可修改）"
        }
    }

    private fun setupAdapters() {
        // 颜色选择器
        colorAdapter = ColorAdapter(getAvailableColors()) { color ->
            selectedColor = color
            selectedColorView.setBackgroundColor(color)
        }
        
        colorRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 6)
            adapter = colorAdapter
        }
        
        // 图标选择器
        iconAdapter = IconAdapter(getAvailableIcons()) { icon ->
            selectedIcon = icon
            setIconResource(selectedIconView, icon)
        }
        
        iconRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = iconAdapter
        }
    }

    private fun setupInitialValues() {
        category?.let { cat ->
            nameEditText.setText(cat.name)
            descriptionEditText.setText(cat.description)
            selectedColor = cat.color
            selectedIcon = cat.icon
            
            selectedColorView.setBackgroundColor(selectedColor)
            setIconResource(selectedIconView, selectedIcon)
            
            colorAdapter.setSelectedColor(selectedColor)
            iconAdapter.setSelectedIcon(selectedIcon)
        } ?: run {
            selectedColorView.setBackgroundColor(selectedColor)
            setIconResource(selectedIconView, selectedIcon)
        }
    }

    private fun saveCategory() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        
        if (name.isBlank() && !isDefaultCategory) {
            nameEditText.error = "分类名称不能为空"
            return
        }
        
        onSaveCallback?.invoke(name, selectedColor, selectedIcon, description)
    }

    private fun getAvailableColors(): List<Int> {
        return listOf(
            0xFF2196F3.toInt(), // 蓝色
            0xFF4CAF50.toInt(), // 绿色
            0xFFFF9800.toInt(), // 橙色
            0xFFE91E63.toInt(), // 粉色
            0xFF9C27B0.toInt(), // 紫色
            0xFFF44336.toInt(), // 红色
            0xFF00BCD4.toInt(), // 青色
            0xFF8BC34A.toInt(), // 浅绿色
            0xFFFFEB3B.toInt(), // 黄色
            0xFF795548.toInt(), // 棕色
            0xFF607D8B.toInt(), // 蓝灰色
            0xFF9E9E9E.toInt()  // 灰色
        )
    }

    private fun getAvailableIcons(): List<String> {
        return listOf(
            "ic_work",
            "ic_person",
            "ic_school",
            "ic_shopping_cart",
            "ic_favorite",
            "ic_home",
            "ic_sports",
            "ic_restaurant",
            "ic_local_hospital",
            "ic_directions_car",
            "ic_flight",
            "ic_music_note",
            "ic_camera",
            "ic_pets",
            "ic_eco",
            "ic_build"
        )
    }

    private fun setIconResource(imageView: ImageView, iconName: String) {
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
        imageView.setImageResource(iconResId)
    }

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            category: Category? = null,
            isDefaultCategory: Boolean = false,
            onSave: (String, Int, String, String) -> Unit
        ) {
            val dialog = CategoryEditDialog().apply {
                this.category = category
                this.isDefaultCategory = isDefaultCategory
                this.onSaveCallback = onSave
            }
            dialog.show(fragmentManager, "CategoryEditDialog")
        }
    }

    // 颜色选择适配器
    private class ColorAdapter(
        private val colors: List<Int>,
        private val onColorSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {
        
        private var selectedPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color_picker, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position], position == selectedPosition)
        }

        override fun getItemCount() = colors.size

        fun setSelectedColor(color: Int) {
            val newPosition = colors.indexOf(color)
            if (newPosition != -1) {
                val oldPosition = selectedPosition
                selectedPosition = newPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
            }
        }

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorView: View = itemView.findViewById(R.id.colorView)
            private val selectedIndicator: View = itemView.findViewById(R.id.selectedIndicator)

            fun bind(color: Int, isSelected: Boolean) {
                colorView.setBackgroundColor(color)
                selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
                
                itemView.setOnClickListener {
                    val oldPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onColorSelected(color)
                }
            }
        }
    }

    // 图标选择适配器
    private class IconAdapter(
        private val icons: List<String>,
        private val onIconSelected: (String) -> Unit
    ) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {
        
        private var selectedPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_icon_picker, parent, false)
            return IconViewHolder(view)
        }

        override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
            holder.bind(icons[position], position == selectedPosition)
        }

        override fun getItemCount() = icons.size

        fun setSelectedIcon(icon: String) {
            val newPosition = icons.indexOf(icon)
            if (newPosition != -1) {
                val oldPosition = selectedPosition
                selectedPosition = newPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
            }
        }

        inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: ImageView = itemView.findViewById(R.id.iconView)
            private val background: View = itemView

            fun bind(iconName: String, isSelected: Boolean) {
                setIconResource(iconView, iconName)
                background.isSelected = isSelected
                
                itemView.setOnClickListener {
                    val oldPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onIconSelected(iconName)
                }
            }
            
            private fun setIconResource(imageView: ImageView, iconName: String) {
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
                imageView.setImageResource(iconResId)
            }
        }
    }
}
