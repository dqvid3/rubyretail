package com.progetto.nomeprogetto.Fragments.MainActivity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.tabs.TabLayout
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Activities.BuyActivity
import com.progetto.nomeprogetto.Adapters.CartAdapter
import com.progetto.nomeprogetto.Adapters.CartAdapterListener
import com.progetto.nomeprogetto.Adapters.WishlistAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Fragments.MainActivity.Home.ProductDetailFragment
import com.progetto.nomeprogetto.Objects.Product
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentCartBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CartFragment : Fragment(), CartAdapterListener {

    override fun restoreCart() {
        binding.emptyCart.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.buyButton.visibility = View.GONE
    }

    private lateinit var binding: FragmentCartBinding
    private val productList = ArrayList<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater)

        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        binding.recyclerView.addItemDecoration(itemDecoration)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position
                binding.emptyCart.visibility = View.GONE
                binding.buyButton.visibility = View.GONE
                if(position==0){ // 0 -> Cart
                    binding.emptyCart.text = "Non hai articoli nel carrello"
                    loadCart(userId)
                }else if(position==1){ // 1 -> WishList
                    binding.emptyCart.text = "Non hai articoli nella wishlist"
                    loadWishList(userId)
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })

        binding.buyButton.setOnClickListener{
            var atleastOneProduct = false
            for(product in productList) {
                if (product.stock != null && product.stock > 0)
                    atleastOneProduct = true
            }
            if (atleastOneProduct)
                startActivity(Intent(requireContext(), BuyActivity::class.java))
            else
                Toast.makeText(requireContext(), "Non hai articoli disponibili nel carrello", Toast.LENGTH_LONG).show()
        }

        return binding.root
    }

    private fun loadCart(userId: Int){
        binding.emptyCart.text = "Non hai articoli nel carrello"
        setProducts(userId,0)

        val adapter = CartAdapter(productList,this,userId)
        binding.recyclerView.adapter = adapter
        adapter.setOnClickListener(object: CartAdapter.OnClickListener{
            override fun onClick(product: Product) =
                setOnClick(product)
        })
    }

    private fun loadWishList(userId: Int){
        binding.emptyCart.text = "Non hai articoli nella wishlist"
        setProducts(userId,1)

        val adapter = WishlistAdapter(productList,this,userId)
        binding.recyclerView.adapter = adapter
        adapter.setOnClickListener(object: WishlistAdapter.OnClickListener{
            override fun onClick(product: Product) =
                setOnClick(product)
        })
    }

    private fun setOnClick(product: Product){
        val bundle = Bundle()
        bundle.putParcelable("product", product)
        val productDetailFragment = ProductDetailFragment()
        productDetailFragment.arguments = bundle
        parentFragmentManager.beginTransaction().hide(this@CartFragment)
            .add(R.id.home_fragment_container,productDetailFragment)
            .commit()
    }

    private fun setProducts(userId: Int,type: Int){
        productList.clear()
        val query: String
        if(type==0) //cart
            query = "SELECT ci.id AS itemId,ci.quantity,pc.stock,pc.color,pc.color_hex," +
                    "p.id,p.name,p.description,p.price,p.width,p.height,p.length," +
                    "p.main_picture_path,p.upload_date,pp.picture_path,ci.color_id," +
                    "IFNULL((SELECT COUNT(*) FROM product_reviews WHERE product_id = p.id),0) AS review_count," +
                    "IFNULL((SELECT AVG(rating) FROM product_reviews WHERE product_id = p.id),0) AS avg_rating," +
                    "s.discount, ROUND(p.price * (1 - IFNULL(s.discount,0) / 100.0), 2) AS discounted_price " +
                    "FROM cart_items ci " +
                    "JOIN products p ON p.id = ci.product_id " +
                    "JOIN product_colors pc ON pc.id = ci.color_id " +
                    "JOIN product_pictures pp ON pp.color_id = pc.id AND pp.picture_index = 0 AND pp.product_id = p.id " +
                    "LEFT JOIN sales s ON s.product_id = p.id AND NOW() BETWEEN s.start_date AND s.end_date " +
                    "WHERE ci.user_id = %s;"
        else query = "SELECT wi.id AS itemId,pc.color,pc.color_hex,pp.picture_path," +
                "p.id,p.name,p.description,p.price,p.width,p.height,p.length," +
                "p.main_picture_path,p.upload_date,wi.color_id," +
                "IFNULL((SELECT COUNT(*) FROM product_reviews WHERE product_id = p.id),0) AS review_count," +
                "IFNULL((SELECT AVG(rating) FROM product_reviews WHERE product_id = p.id),0) AS avg_rating," +
                "s.discount, ROUND(p.price * (1 - IFNULL(s.discount,0) / 100.0), 2) AS discounted_price " +
                "FROM wishlist_items wi " +
                "JOIN products p ON p.id = wi.product_id " +
                "JOIN product_colors pc ON pc.id = wi.color_id " +
                "JOIN product_pictures pp ON pp.color_id = pc.id AND pp.picture_index = 0 AND pp.product_id = p.id " +
                "LEFT JOIN sales s ON s.product_id = p.id AND NOW() BETWEEN s.start_date AND s.end_date " +
                "WHERE wi.user_id = %s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    var loadedProducts = 0
                    val productsArray = response.body()?.getAsJsonArray("queryset")
                    if (productsArray != null && productsArray.size() > 0) {
                        for (i in 0 until productsArray.size()) {
                            val productObject = productsArray[i].asJsonObject
                            val id = productObject.get("id").asInt
                            val name = productObject.get("name").asString
                            val description = productObject.get("description").asString
                            val price = productObject.get("price").asDouble
                            val width = productObject.get("width").asDouble
                            val height = productObject.get("height").asDouble
                            val length = productObject.get("length").asDouble
                            val avgRating = productObject.get("avg_rating").asDouble
                            val reviewsNumber = productObject.get("review_count").asInt
                            val date = productObject.get("upload_date").asString
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            val uploadDate = LocalDateTime.parse(date, formatter)
                            val main_picture_path = productObject.get("main_picture_path").asString
                            val color = productObject.get("color").asString
                            val itemId = productObject.get("itemId").asInt
                            val color_hex = productObject.get("color_hex").asString
                            val picture_path = productObject.get("picture_path").asString
                            val colorId = productObject.get("color_id").asInt
                            val discountElem = productObject.get("discount")
                            val discount = if (discountElem == null || discountElem.isJsonNull) null else discountElem.asInt
                            val discountedPrice = productObject.get("discounted_price").asDouble
                            var stock: Int? = null
                            var quantity: Int? = null
                            if(type == 0){
                                stock = productObject.get("stock").asInt
                                quantity = productObject.get("quantity").asInt
                                if (stock>0 && quantity>stock){
                                    quantity = stock
                                    val updateQuery = "UPDATE cart_items set quantity=%s where id=%s;"
                                    val updateParams = com.google.gson.JsonArray().apply { add(quantity); add(itemId) }.toString()
                                    ClientNetwork.retrofit.updateSafe(updateQuery, updateParams).enqueue(object : Callback<JsonObject> {
                                        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {}
                                        override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                            Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                                    })
                                }
                            }
                            var main_picture : Bitmap? = null
                            var picture : Bitmap? = null
                            for(j in 0..1) {
                                ClientNetwork.retrofit.image(if (j==0) main_picture_path else picture_path).enqueue(object : Callback<ResponseBody> {
                                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                        if (response.isSuccessful) {
                                            if (response.body() != null) {
                                                if(j==0) main_picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                                else picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                                if(j==1) {
                                                    val product = Product(id, name, description, price, width, height,
                                                        length, main_picture, avgRating, reviewsNumber, uploadDate,
                                                        itemId, color, color_hex, quantity, stock, picture, colorId,
                                                        discount = discount, discounted_price = discountedPrice)
                                                    productList.add(product)
                                                    loadedProducts++
                                                    if (loadedProducts == productsArray.size()) {
                                                        if(type==0){
                                                            for (p in productList) {
                                                                if (p.stock != null && p.stock>0) {
                                                                    binding.buyButton.visibility = View.VISIBLE
                                                                    break
                                                                }
                                                            }
                                                        }
                                                        binding.emptyCart.visibility = View.GONE
                                                        binding.recyclerView.visibility = View.VISIBLE
                                                        binding.recyclerView.adapter?.notifyDataSetChanged()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) =
                                        Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                                })
                            }
                        }
                    }else restoreCart()
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        when(binding.tabLayout.selectedTabPosition){
            0 -> {
                loadCart(userId)
            }
            1 -> {
                loadWishList(userId)
            }
        }
    }
}