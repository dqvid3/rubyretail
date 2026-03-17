package com.progetto.nomeprogetto.Fragments.MainActivity.Home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Adapters.CategoryImageAdapter
import com.progetto.nomeprogetto.Adapters.ProductAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Objects.Product
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentHomeBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater)

        // Categories
        binding.recyclerViewCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val categoriesList = HashMap<String, Bitmap>()
        setCategories(categoriesList)
        val density = resources.displayMetrics.density
        // Section = 1/3 screen, subtract header (~42dp), label (~24dp), padding (~32dp)
        val itemSizePx = (resources.displayMetrics.heightPixels / 3f - (42 + 24 + 32) * density).toInt()
        val imageAdapter = CategoryImageAdapter(categoriesList, itemSizePx)
        binding.recyclerViewCategory.adapter = imageAdapter
        imageAdapter.setOnClickListener(object : CategoryImageAdapter.OnClickListener {
            override fun onClick(categoryName: String) {
                val bundle = Bundle()
                bundle.putString("category_name", categoryName)
                val productFragment = ProductFragment()
                productFragment.arguments = bundle
                parentFragmentManager.beginTransaction().hide(this@HomeFragment)
                    .add(R.id.home_fragment_home_container, productFragment)
                    .commit()
            }
        })

        // Sales
        binding.recyclerViewSales.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val saleProducts = ArrayList<Product>()
        setSales(saleProducts)
        val saleAdapter = ProductAdapter(saleProducts, true)
        binding.recyclerViewSales.adapter = saleAdapter
        saleAdapter.setOnClickListener(object : ProductAdapter.OnClickListener {
            override fun onClick(product: Product) {
                val bundle = Bundle()
                bundle.putParcelable("product", product)
                val productDetailFragment = ProductDetailFragment()
                productDetailFragment.arguments = bundle
                parentFragmentManager.beginTransaction().hide(this@HomeFragment)
                    .add(R.id.home_fragment_home_container, productDetailFragment)
                    .commit()
            }
        })

        // New arrivals
        binding.recyclerViewNews.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val newProducts = ArrayList<Product>()
        setProducts(newProducts)
        val productAdapter = ProductAdapter(newProducts, true)
        binding.recyclerViewNews.adapter = productAdapter
        productAdapter.setOnClickListener(object : ProductAdapter.OnClickListener {
            override fun onClick(product: Product) {
                val bundle = Bundle()
                bundle.putParcelable("product", product)
                val productDetailFragment = ProductDetailFragment()
                productDetailFragment.arguments = bundle
                parentFragmentManager.beginTransaction().hide(this@HomeFragment)
                    .add(R.id.home_fragment_home_container, productDetailFragment)
                    .commit()
            }
        })

        return binding.root
    }

    private fun setCategories(categoriesList: HashMap<String, Bitmap>) {
        val query = "SELECT name,picture_path FROM categories"

        ClientNetwork.retrofit.select(query).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val categoriesArray = response.body()?.getAsJsonArray("queryset")
                    if (categoriesArray != null && categoriesArray.size() > 0) {
                        var loadedCategories = 0
                        for (i in 0 until categoriesArray.size()) {
                            val categoryObject = categoriesArray[i].asJsonObject
                            val picture_path = categoryObject.get("picture_path").asString
                            val name = categoryObject.get("name").asString
                            ClientNetwork.retrofit.image(picture_path).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful && response.body() != null) {
                                        val picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                        categoriesList[name] = picture
                                    }
                                    loadedCategories++
                                    if (loadedCategories == categoriesArray.size()) {
                                        binding.emptyCategories.visibility = View.GONE
                                        binding.headerCategories.visibility = View.VISIBLE
                                        binding.recyclerViewCategory.visibility = View.VISIBLE
                                        binding.recyclerViewCategory.adapter?.notifyDataSetChanged()
                                    }
                                }
                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    loadedCategories++
                                    if (loadedCategories == categoriesArray.size()) {
                                        binding.emptyCategories.visibility = View.GONE
                                        binding.headerCategories.visibility = View.VISIBLE
                                        binding.recyclerViewCategory.visibility = View.VISIBLE
                                        binding.recyclerViewCategory.adapter?.notifyDataSetChanged()
                                    }
                                }
                            })
                        }
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setSales(productList: ArrayList<Product>) {
        val query = "SELECT p.id, p.name, p.description, p.price, " +
                "ROUND(p.price * (1 - s.discount / 100.0), 2) AS discounted_price, " +
                "s.discount, s.description AS discount_desc, " +
                "p.width, p.height, p.length, p.main_picture_path, p.upload_date, " +
                "IFNULL((SELECT COUNT(*) FROM product_reviews WHERE product_id = p.id),0) AS review_count, " +
                "IFNULL((SELECT AVG(rating) FROM product_reviews WHERE product_id = p.id),0) AS avg_rating " +
                "FROM products p JOIN sales s ON p.id = s.product_id " +
                "WHERE NOW() BETWEEN s.start_date AND s.end_date " +
                "ORDER BY s.discount DESC LIMIT 20;"

        ClientNetwork.retrofit.select(query).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val productsArray = response.body()?.getAsJsonArray("queryset")
                    if (productsArray != null && productsArray.size() > 0) {
                        var loadedProducts = 0
                        for (i in 0 until productsArray.size()) {
                            val obj = productsArray[i].asJsonObject
                            val id = obj.get("id").asInt
                            val name = obj.get("name").asString
                            val description = obj.get("description").asString
                            val price = obj.get("price").asDouble
                            val discountedPrice = obj.get("discounted_price").asDouble
                            val discount = obj.get("discount").asInt
                            val discountDesc = obj.get("discount_desc").asString
                            val width = obj.get("width").asDouble
                            val height = obj.get("height").asDouble
                            val length = obj.get("length").asDouble
                            val avgRating = obj.get("avg_rating").asDouble
                            val reviewsNumber = obj.get("review_count").asInt
                            val date = obj.get("upload_date").asString
                            val uploadDate = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val picturePath = obj.get("main_picture_path").asString

                            ClientNetwork.retrofit.image(picturePath).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful && response.body() != null) {
                                        val picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                        productList.add(Product(id, name, description, price, width, height, length,
                                            picture, avgRating, reviewsNumber, uploadDate,
                                            discount = discount, discount_desc = discountDesc, discounted_price = discountedPrice))
                                    }
                                    loadedProducts++
                                    if (loadedProducts == productsArray.size()) {
                                        binding.headerSales.visibility = View.VISIBLE
                                        binding.recyclerViewSales.visibility = View.VISIBLE
                                        binding.recyclerViewSales.adapter?.notifyDataSetChanged()
                                    }
                                }
                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    loadedProducts++
                                    if (loadedProducts == productsArray.size()) {
                                        binding.headerSales.visibility = View.VISIBLE
                                        binding.recyclerViewSales.visibility = View.VISIBLE
                                        binding.recyclerViewSales.adapter?.notifyDataSetChanged()
                                    }
                                }
                            })
                        }
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setProducts(productList: ArrayList<Product>) {
        val query = "SELECT p.id,p.name,p.description,p.price,p.width,p.height,p.length,p.main_picture_path,p.upload_date," +
                "IFNULL((SELECT COUNT(*) FROM product_reviews WHERE product_id = p.id),0) AS review_count," +
                "IFNULL((SELECT AVG(rating) FROM product_reviews WHERE product_id = p.id),0) AS avg_rating," +
                "s.discount, ROUND(p.price * (1 - IFNULL(s.discount,0) / 100.0), 2) AS discounted_price " +
                "FROM products p LEFT JOIN sales s ON s.product_id = p.id AND NOW() BETWEEN s.start_date AND s.end_date " +
                "WHERE p.upload_date >= DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY p.upload_date DESC LIMIT 20;"

        ClientNetwork.retrofit.select(query).enqueue(object : Callback<JsonObject> {
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
                            val uploadDate = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val main_picture_path = productObject.get("main_picture_path").asString
                            val discountElem = productObject.get("discount")
                            val discount = if (discountElem == null || discountElem.isJsonNull) null else discountElem.asInt
                            val discountedPrice = productObject.get("discounted_price").asDouble
                            ClientNetwork.retrofit.image(main_picture_path).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful && response.body() != null) {
                                        val main_picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                        productList.add(Product(id, name, description, price, width, height, length, main_picture, avgRating, reviewsNumber, uploadDate,
                                            discount = discount, discounted_price = discountedPrice))
                                    }
                                    loadedProducts++
                                    if (loadedProducts == productsArray.size()) {
                                        binding.headerNews.visibility = View.VISIBLE
                                        binding.recyclerViewNews.visibility = View.VISIBLE
                                        binding.recyclerViewNews.adapter?.notifyDataSetChanged()
                                    }
                                }
                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    loadedProducts++
                                    if (loadedProducts == productsArray.size()) {
                                        binding.headerNews.visibility = View.VISIBLE
                                        binding.recyclerViewNews.visibility = View.VISIBLE
                                        binding.recyclerViewNews.adapter?.notifyDataSetChanged()
                                    }
                                }
                            })
                        }
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}
