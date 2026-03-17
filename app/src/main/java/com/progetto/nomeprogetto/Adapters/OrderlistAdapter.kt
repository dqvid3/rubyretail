package com.progetto.nomeprogetto.Adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.progetto.nomeprogetto.Objects.OrderItem
import com.progetto.nomeprogetto.databinding.OrderItemViewDesignBinding

class OrderlistAdapter(private var orderList: List<OrderItem>) : RecyclerView.Adapter<OrderlistAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    class ViewHolder(binding: OrderItemViewDesignBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView = binding.imageView
        val productName = binding.productName
        val orderDate = binding.orderDate
        val orderNumber = binding.orderNumber
        val infoImage = binding.infoImage
        val priceLayout = binding.priceLayout
        val productPrice = binding.productPrice
        val originalPrice = binding.originalPrice
        val productQty = binding.productQty
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = OrderItemViewDesignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = orderList[position]

        holder.imageView.setImageBitmap(item.picture)
        holder.productName.text = item.name
        holder.orderDate.text = "Ordinato il " + item.orderDate.toString()
        holder.orderNumber.text = "Numero ordine #" + item.orderId
        if (item.price != null && item.quantity != null) {
            holder.orderNumber.visibility = View.GONE
            holder.orderDate.visibility = View.GONE
            holder.infoImage.visibility = View.GONE
            holder.productPrice.text = String.format("%.2f €", item.price)
            holder.productQty.text = "Quantità " + item.quantity.toString()
            holder.priceLayout.visibility = View.VISIBLE
            holder.productQty.visibility = View.VISIBLE
            if (item.originalPrice != null && item.originalPrice > item.price) {
                holder.originalPrice.text = String.format("%.2f €", item.originalPrice)
                holder.originalPrice.paintFlags = holder.originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.originalPrice.visibility = View.VISIBLE
            } else {
                holder.originalPrice.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            onClickListener?.onClick(item)
        }
    }

    interface OnClickListener {
        fun onClick(item: OrderItem)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
}
