package com.progetto.nomeprogetto.Adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.progetto.nomeprogetto.Objects.Product
import com.progetto.nomeprogetto.databinding.ProductViewDesignBinding

class ProductAdapter(private var productList: List<Product>, private val isHorizontal: Boolean = false) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    class ViewHolder(binding: ProductViewDesignBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView = binding.imageView
        val productName = binding.productName
        val avgRating = binding.avgRating
        val ratingBar = binding.ratingBar
        val price = binding.price
        val reviewsNumber = binding.reviewsNumber
        val originalPrice = binding.originalPrice
        val discountBadge = binding.discountBadge
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ProductViewDesignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        if (isHorizontal) {
            view.root.layoutParams = view.root.layoutParams.apply {
                width = (parent.width * 0.82f).toInt()
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.imageView.setImageBitmap(product.main_picture)
        holder.productName.text = product.name
        holder.avgRating.text = product.avgRating.toString()
        holder.ratingBar.rating = product.avgRating.toFloat()
        holder.reviewsNumber.text = "(" + product.reviewsNumber.toString() + ")"

        if (product.discount != null) {
            val discounted = String.format("%.2f €", product.discounted_price)
            holder.price.text = discounted
            holder.originalPrice.text = String.format("%.2f €", product.price)
            holder.originalPrice.paintFlags = holder.originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.originalPrice.visibility = View.VISIBLE
            holder.discountBadge.text = "-${product.discount}%"
            holder.discountBadge.visibility = View.VISIBLE
        } else {
            holder.price.text = product.price.toString() + " €"
            holder.originalPrice.visibility = View.GONE
            holder.discountBadge.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onClickListener?.onClick(product)
        }
    }

    interface OnClickListener {
        fun onClick(product: Product)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
}