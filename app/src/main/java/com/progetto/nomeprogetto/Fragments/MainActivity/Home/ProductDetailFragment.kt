package com.progetto.nomeprogetto.Fragments.MainActivity.Home

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.google.android.material.card.MaterialCardView
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Activities.MainActivity
import com.progetto.nomeprogetto.Adapters.ProductColorAdapter
import com.progetto.nomeprogetto.Adapters.ProductImageAdapter
import com.progetto.nomeprogetto.Adapters.ProductReviewAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Objects.Product
import com.progetto.nomeprogetto.Objects.ProductColor
import com.progetto.nomeprogetto.Objects.ProductReview
import com.progetto.nomeprogetto.Objects.User
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentProductDetailBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProductDetailFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailBinding
    private var colorSelected: Int = -1
    private var currentStock: Int = -1
    private var addToWish = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductDetailBinding.inflate(inflater)

        val product: Product? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("product", Product::class.java)
        } else
            arguments?.getParcelable("product")

        val colorList = ArrayList<ProductColor>()
        val imageList = HashMap<Int, Bitmap>()
        val reviewList = ArrayList<ProductReview>()
        val colorAdapter = ProductColorAdapter(colorList)
        val imageAdapter = ProductImageAdapter(imageList)
        val reviewAdapter = ProductReviewAdapter(reviewList)
        binding.recyclerColorView.adapter = colorAdapter
        binding.recyclerImageView.adapter = imageAdapter
        binding.recyclerReviewsView.adapter = reviewAdapter

        //lista immagini
        LinearSnapHelper().attachToRecyclerView(binding.recyclerImageView)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerImageView.layoutManager = layoutManager
        binding.recyclerImageView.setOnFlingListener(null)
        layoutManager.isSmoothScrollbarEnabled = true

        //lista colori
        binding.recyclerColorView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        setColors(product?.id, colorList, imageList)

        //lista recensioni
        binding.recyclerReviewsView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        binding.recyclerReviewsView.addItemDecoration(itemDecoration)
        setReviews(product?.id, reviewList)

        colorAdapter.setOnClickListener(object : ProductColorAdapter.OnClickListener {
            override fun onClick(position: Int) {
                val prevPos = colorAdapter.getPosition()
                colorSelected = colorList.get(position).color_id
                setWishButton(colorSelected)
                currentStock = colorList.get(position).stock
                if (currentStock==0)
                    outOfStock()
                else
                    productAvailable()
                colorAdapter.setColor(colorSelected)
                colorAdapter.notifyItemChanged(position)
                colorAdapter.notifyItemChanged(prevPos)
                setImages(product?.id, imageList, colorSelected)
                val quantityOptions = (1..currentStock).toList()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, quantityOptions)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerQty.adapter = adapter
            }
        })

        binding.backButton.setOnClickListener {
            val productFragment = parentFragmentManager.findFragmentByTag("ProductFragment")
            val cartFragment = parentFragmentManager.findFragmentByTag("CartFragment")
            if (productFragment != null)
                parentFragmentManager.beginTransaction()
                    .remove(this).show(productFragment)
                    .commit()
            else if (cartFragment != null) {
                parentFragmentManager.beginTransaction()
                    .remove(this).show(cartFragment)
                    .commit()
                cartFragment.onResume()
            }
            else // sto aprendo il prodotto dalle novità
                parentFragmentManager.beginTransaction().replace(R.id.home_fragment_home_container, HomeFragment())
                    .commit()
        }

        binding.addToCart.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            val userId = sharedPref.getInt("ID", 0)
            val qty = binding.spinnerQty.selectedItem.toString().toInt()
            addToCart(userId, qty, colorSelected, product?.id ?: 0)
        }

        binding.addToWish.setOnClickListener{
            if(!addToWish) {
                binding.addToWish.imageTintList = ColorStateList.valueOf(Color.RED)
                addToWish = true
                addToWish(colorSelected, product?.id ?: 0)
            }else{
                binding.addToWish.imageTintList = ColorStateList.valueOf(Color.GRAY)
                addToWish = false
                removeFromWish(colorSelected)
            }
        }

        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        hasUserBought(userId,product?.id)

        binding.addReviewButton.setOnClickListener{
            if (binding.productRating.rating == 0.0f)
                Toast.makeText(requireContext(), "Scegli un voto da dare al prodotto prima", Toast.LENGTH_LONG).show()
            else if (binding.reviewText.text.isBlank())
                Toast.makeText(requireContext(), "Insersici un tuo pensiero sul prodotto prima", Toast.LENGTH_LONG).show()
            else
                addReview(userId,product?.id,reviewList)
        }

        binding.productName.text = product?.name
        binding.productDescription.text = product?.description
        if (product?.discount != null) {
            binding.productPrice.text = String.format("%.2f € (-%d%%)", product.discounted_price, product.discount)
        } else {
            binding.productPrice.text = String.format("%.2f €", product?.price)
        }
        binding.productSpecs.text = "Larghezza: ${product?.width}\nLunghezza: ${product?.length}\nAltezza: ${product?.height}"

        return binding.root
    }

    private fun hasUserBought(userId: Int,productId: Int?){
        val query = "SELECT EXISTS (SELECT 1 FROM orders o, order_items oi, product_colors pc " +
                "WHERE o.id = oi.order_id AND oi.color_id = pc.id " +
                "AND o.user_id = %s AND pc.product_id = %s) AS has_bought;"
        val params = com.google.gson.JsonArray().apply { add(userId); add(productId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val hasBought = response.body()?.getAsJsonArray("queryset")?.get(0)
                        ?.asJsonObject?.get("has_bought")?.asInt
                    if (hasBought==1)
                        hasUserReviewed(userId,productId)
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun hasUserReviewed(userId: Int,productId: Int?){
        val query = "SELECT EXISTS (SELECT 1 FROM product_reviews WHERE product_id = %s AND user_id = %s) AS has_reviewed;"
        val params = com.google.gson.JsonArray().apply { add(productId); add(userId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val hasReviewed = response.body()?.getAsJsonArray("queryset")?.get(0)
                        ?.asJsonObject?.get("has_reviewed")?.asInt
                    if (hasReviewed==1)
                        binding.addReview.visibility = View.GONE
                    else
                        binding.addReview.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun outOfStock(){
        binding.qtyText.visibility = View.GONE
        binding.spinnerQty.visibility = View.GONE
        binding.noStock.visibility = View.VISIBLE
        binding.addToCart.visibility = View.GONE
    }

    private fun productAvailable(){
        binding.qtyText.visibility = View.VISIBLE
        binding.spinnerQty.visibility = View.VISIBLE
        binding.noStock.visibility = View.GONE
        binding.addToCart.visibility = View.VISIBLE
    }

    private fun addReview(userId: Int,productId: Int?, reviewList: ArrayList<ProductReview>){
        val query = "INSERT INTO product_reviews (product_id,user_id,rating,comment) VALUES (%s,%s,%s,%s);"
        val params = com.google.gson.JsonArray().apply {
            add(productId); add(userId); add(binding.productRating.rating.toInt()); add(binding.reviewText.text.toString())
        }.toString()

        ClientNetwork.retrofit.insertSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Recensione effettuata con successo", Toast.LENGTH_SHORT).show()
                    binding.addReview.visibility = View.GONE
                    setReviews(productId, reviewList)
                }else
                    Toast.makeText(requireContext(), "Errore nell'inserimento dell'articolo, riprova", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun addToCart(userId: Int, qty: Int, colorId: Int, productId: Int){
        val checkQuery = "SELECT id,quantity from cart_items where user_id=%s and color_id=%s;"
        val checkParams = com.google.gson.JsonArray().apply { add(userId); add(colorId) }.toString()

        ClientNetwork.retrofit.selectSafe(checkQuery, checkParams).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val itemsArray = response.body()?.getAsJsonArray("queryset")
                    if (itemsArray != null && itemsArray.size() > 0) {
                        val itemObject = itemsArray[0].asJsonObject
                        val quantity = itemObject.get("quantity").asInt
                        val itemId = itemObject.get("id").asInt
                        if(quantity+qty>currentStock)
                            Toast.makeText(requireContext(), "Non puoi inserire più della quantità disponibile per prodotto", Toast.LENGTH_LONG).show()
                        else{
                            val updateQuery = "UPDATE cart_items set quantity=%s where id=%s;"
                            val updateParams = com.google.gson.JsonArray().apply { add(quantity+qty); add(itemId) }.toString()
                            ClientNetwork.retrofit.updateSafe(updateQuery, updateParams).enqueue(object : Callback<JsonObject> {
                                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                    if(response.isSuccessful)
                                        Toast.makeText(requireContext(), "Articoli aggiunti al carrello $qty",Toast.LENGTH_SHORT).show()
                                    else
                                        Toast.makeText(requireContext(), "Errore nell'inserimento dell'articolo, riprova",Toast.LENGTH_SHORT).show()
                                }
                                override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                    Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                            })
                        }
                    }else{
                        val insertQuery = "INSERT INTO cart_items (user_id,product_id,quantity,color_id) VALUES (%s,%s,%s,%s);"
                        val insertParams = com.google.gson.JsonArray().apply { add(userId); add(productId); add(qty); add(colorId) }.toString()
                        ClientNetwork.retrofit.insertSafe(insertQuery, insertParams).enqueue(object : Callback<JsonObject> {
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                if (response.isSuccessful)
                                    Toast.makeText(requireContext(), "Articoli aggiunti al carrello $qty", Toast.LENGTH_SHORT).show()
                                else
                                    Toast.makeText(requireContext(), "Errore nell'inserimento dell'articolo, riprova", Toast.LENGTH_SHORT).show()
                            }
                            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                        })
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun addToWish(colorId: Int, productId: Int){
        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        val checkQuery = "SELECT id from wishlist_items where user_id=%s and color_id=%s;"
        val checkParams = com.google.gson.JsonArray().apply { add(userId); add(colorId) }.toString()

        ClientNetwork.retrofit.selectSafe(checkQuery, checkParams).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val itemsArray = response.body()?.getAsJsonArray("queryset")
                    if (itemsArray != null && itemsArray.size() > 0) {
                        Toast.makeText(requireContext(), "Articolo già presente nella wishlist", Toast.LENGTH_LONG).show()
                    }else{
                        val insertQuery = "INSERT INTO wishlist_items (user_id,product_id,color_id) VALUES (%s,%s,%s);"
                        val insertParams = com.google.gson.JsonArray().apply { add(userId); add(productId); add(colorId) }.toString()
                        ClientNetwork.retrofit.insertSafe(insertQuery, insertParams).enqueue(object : Callback<JsonObject> {
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                if (response.isSuccessful)
                                    Toast.makeText(requireContext(), "Articolo aggiunto alla wishlist", Toast.LENGTH_SHORT).show()
                                else
                                    Toast.makeText(requireContext(), "Errore nell'inserimento dell'articolo, riprova", Toast.LENGTH_SHORT).show()
                            }
                            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                        })
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun removeFromWish(colorId: Int){
        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        val query = "DELETE FROM wishlist_items where user_id=%s and color_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId); add(colorId) }.toString()

        ClientNetwork.retrofit.removeSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) =
                Toast.makeText(context, "Articolo rimosso dalla wishlist", Toast.LENGTH_LONG).show()
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun setColors(productId: Int?,colorList: ArrayList<ProductColor>,imageList: HashMap<Int, Bitmap>){
        val query = "SELECT id,pc.color AS color_name,stock, pc.color_hex AS color_hex FROM product_colors pc WHERE pc.product_id = %s;"
        val params = com.google.gson.JsonArray().apply { add(productId) }.toString()
        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val colorsArray = response.body()?.getAsJsonArray("queryset")
                    if (colorsArray != null && colorsArray.size() > 0) {
                        binding.recyclerColorView.visibility = View.VISIBLE
                        var loadedColors = 0
                        for (i in 0 until colorsArray.size()) {
                            val colorObject = colorsArray[i].asJsonObject
                            val color_id = colorObject.get("id").asInt
                            val colorName = colorObject.get("color_name").asString
                            val colorHex = colorObject.get("color_hex").asString
                            val stock = colorObject.get("stock").asInt
                            colorList.add(ProductColor(colorName,color_id,colorHex,stock))
                            if(i==0){
                                if (stock==0)
                                    outOfStock()
                                currentStock = stock
                                colorSelected = color_id
                                setWishButton(colorSelected)
                                setImages(productId, imageList,colorSelected)
                                val quantityOptions = (1..stock).toList()
                                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, quantityOptions)
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.spinnerQty.adapter = adapter
                            }
                            loadedColors++
                            if(loadedColors==colorsArray.size())
                                binding.recyclerColorView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun setWishButton(colorId: Int){
        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        val query = "SELECT id from wishlist_items where user_id=%s and color_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId); add(colorId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val itemsArray = response.body()?.getAsJsonArray("queryset")
                    if (itemsArray != null && itemsArray.size() > 0) {
                        binding.addToWish.imageTintList = ColorStateList.valueOf(Color.RED)
                        addToWish = true
                    }else{
                        binding.addToWish.imageTintList = ColorStateList.valueOf(Color.GRAY)
                        addToWish = false
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(context, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun setImages(productId: Int?, imageList: HashMap<Int, Bitmap>, colorSelected: Int) {
        val query = "SELECT picture_path,picture_index FROM product_pictures WHERE product_id = %s and color_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(productId); add(colorSelected) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val picturesArray = response.body()?.getAsJsonArray("queryset")
                    if (picturesArray != null && picturesArray.size() > 0) {
                        var loadedImages = 0
                        for (i in 0 until picturesArray.size()) {
                            val pictureObject = picturesArray[i].asJsonObject
                            val picture_path = pictureObject.get("picture_path").asString
                            val pictureIndex = pictureObject.get("picture_index").asInt
                            ClientNetwork.retrofit.image(picture_path).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        if (response.body() != null) {
                                            val picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                            imageList[pictureIndex] = picture
                                            loadedImages++
                                            if (loadedImages == picturesArray.size())
                                                binding.recyclerImageView.adapter?.notifyDataSetChanged()
                                        }
                                    }
                                }
                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) =
                                    Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                            })
                        }
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun setReviews(productId: Int?, reviewList: ArrayList<ProductReview>) {
        val query = "SELECT pr.rating, pr.comment, pr.review_date, u.username FROM product_reviews pr, users u " +
                "WHERE pr.user_id = u.id and pr.product_id = %s;"
        val params = com.google.gson.JsonArray().apply { add(productId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val reviewsArray = response.body()?.getAsJsonArray("queryset")
                    if (reviewsArray != null && reviewsArray.size() > 0) {
                        binding.emptyReviews.visibility = View.GONE
                        var loadedReviews = 0
                        for (i in 0 until reviewsArray.size()) {
                            val reviewObject = reviewsArray[i].asJsonObject
                            val username = reviewObject.get("username").asString
                            val rating = reviewObject.get("rating").asInt
                            val comment = reviewObject.get("comment").asString
                            val date = reviewObject.get("review_date").asString
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            val reviewDate = LocalDateTime.parse(date, formatter)
                            val productReview = ProductReview(username, rating, comment, reviewDate)
                            reviewList.add(productReview)
                            loadedReviews++
                            if (loadedReviews == reviewsArray.size())
                                binding.recyclerReviewsView.adapter?.notifyDataSetChanged()
                        }
                    }else binding.emptyReviews.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }
}