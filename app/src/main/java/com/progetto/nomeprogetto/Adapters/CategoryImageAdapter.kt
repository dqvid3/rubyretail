package com.progetto.nomeprogetto.Adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.progetto.nomeprogetto.databinding.CategoryImageViewDesignBinding

class CategoryImageAdapter(private var categoriesList: HashMap<String,Bitmap>, private val itemSizePx: Int = 0) : RecyclerView.Adapter<CategoryImageAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    class ViewHolder(binding: CategoryImageViewDesignBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView = binding.imageView
        val categoryName = binding.categoryName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CategoryImageViewDesignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        if (itemSizePx > 0) {
            view.root.layoutParams = view.root.layoutParams.apply {
                height = itemSizePx
                width = itemSizePx
            }
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryName = categoriesList.keys.toList()[position]

        holder.categoryName.text = categoryName
        holder.imageView.setImageBitmap(categoriesList[categoryName])

        holder.itemView.setOnClickListener {
            onClickListener?.onClick(categoryName)
        }
    }

    interface OnClickListener {
        fun onClick(categoryName: String)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
}