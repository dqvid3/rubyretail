package com.progetto.nomeprogetto.Adapters

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Objects.Product
import com.progetto.nomeprogetto.databinding.CartViewDesignBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartAdapter(private val productList: List<Product>,private val listener: CartAdapterListener?,private val userId: Int?) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    class ViewHolder(binding: CartViewDesignBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView = binding.imageView
        val productName = binding.productName
        val price = binding.price
        val originalPrice = binding.originalPrice
        val discountBadge = binding.discountBadge
        val spinnerQty = binding.spinnerQty
        val colorName = binding.colorName
        val colorView = binding.colorView
        val removeProduct = binding.removeProduct
        val addToWish = binding.addToWish
        val qtyLayout = binding.qtyLayout
        val noStock = binding.noStock
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CartViewDesignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val product = productList[position]

        holder.imageView.setImageBitmap(product.picture)
        holder.productName.text = product.name
        if (product.discount != null) {
            holder.price.text = String.format("%.2f €", product.discounted_price)
            holder.originalPrice.text = String.format("%.2f €", product.price)
            holder.originalPrice.paintFlags = holder.originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.originalPrice.visibility = View.VISIBLE
            holder.discountBadge.text = "-${product.discount}%"
            holder.discountBadge.visibility = View.VISIBLE
        } else {
            holder.price.text = String.format("%.2f €", product.price)
            holder.originalPrice.visibility = View.GONE
            holder.discountBadge.visibility = View.GONE
        }
        holder.colorName.text = product.colorName
        holder.colorView.backgroundTintList = ColorStateList.valueOf(Color.parseColor(product.color_hex))

        if (product.stock==0){
            holder.qtyLayout.visibility = View.GONE
            holder.noStock.visibility = View.VISIBLE
        }
        val quantityOptions = (1..product.stock!!).toList()
        val adapter = ArrayAdapter(holder.itemView.context, R.layout.simple_spinner_item, quantityOptions)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        holder.spinnerQty.adapter = adapter
        val quantityIndex = quantityOptions.indexOf(product.quantity)
        if (quantityIndex != -1)
            holder.spinnerQty.setSelection(quantityIndex)
        holder.spinnerQty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var isInitialSelection = true
            override fun onItemSelected(parent: AdapterView<*>, view: View?, p: Int, id: Long) {
                if(isInitialSelection){
                    isInitialSelection = false
                    return
                }
                val selectedQuantity = quantityOptions[p]
                updateCart(position,selectedQuantity,holder.itemView.context)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        holder.removeProduct.setOnClickListener{
            if (listener==null && productList.size==1)
                Toast.makeText(holder.itemView.context, "Non puoi rimuovere l'ultimo articolo che rimane", Toast.LENGTH_LONG).show()
            else
                product.itemId?.let { removeFromCart(it,position,holder.itemView.context) }
        }

        holder.addToWish.setOnClickListener{
            if (product.colorId != null)
                addToWishlist(product.colorId, product.id, holder.itemView.context)
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

    private fun removeItem(position: Int) {
        (productList as MutableList<Product>).removeAt(position)
        notifyItemRemoved(position)
        if (productList.isEmpty()) {
            listener?.restoreCart()
        } else
            notifyItemRangeChanged(position, productList.size)
    }

    private fun updateCart(position: Int,quantity: Int,context: Context){
        val product = productList[position]
        val query = "UPDATE cart_items set quantity=%s where id=%s;"
        val params = com.google.gson.JsonArray().apply { add(quantity); add(product.itemId) }.toString()

        ClientNetwork.retrofit.updateSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                product.quantity = quantity
                Toast.makeText(context, "Quantità aggiornata", Toast.LENGTH_LONG).show()
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun removeFromCart(itemId: Int,position: Int,context: Context){
        val query = "DELETE FROM cart_items WHERE id=%s;"
        val params = com.google.gson.JsonArray().apply { add(itemId) }.toString()

        ClientNetwork.retrofit.removeSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) = removeItem(position)
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun addToWishlist(colorId: Int, productId: Int, context: Context){
        val checkQuery = "SELECT id from wishlist_items where user_id=%s and color_id=%s;"
        val checkParams = com.google.gson.JsonArray().apply { add(userId); add(colorId) }.toString()

        ClientNetwork.retrofit.selectSafe(checkQuery, checkParams).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val itemsArray = response.body()?.getAsJsonArray("queryset")
                    if (itemsArray != null && itemsArray.size() > 0) {
                        Toast.makeText(context, "Articolo già presente nella wishlist", Toast.LENGTH_LONG).show()
                    }else{
                        val insertQuery = "INSERT INTO wishlist_items (user_id,product_id,color_id) VALUES (%s,%s,%s);"
                        val insertParams = com.google.gson.JsonArray().apply { add(userId); add(productId); add(colorId) }.toString()
                        ClientNetwork.retrofit.insertSafe(insertQuery, insertParams).enqueue(object : Callback<JsonObject> {
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                if (response.isSuccessful)
                                    Toast.makeText(context, "Articolo aggiunto alla wishlist", Toast.LENGTH_SHORT).show()
                                else
                                    Toast.makeText(context, "Errore nell'inserimento dell'articolo, riprova", Toast.LENGTH_SHORT).show()
                            }
                            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                        })
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }
}