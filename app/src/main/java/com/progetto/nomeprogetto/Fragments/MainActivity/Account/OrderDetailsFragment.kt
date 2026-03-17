package com.progetto.nomeprogetto.Fragments.MainActivity.Account

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Adapters.OrderlistAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Fragments.MainActivity.Home.ProductFragment
import com.progetto.nomeprogetto.Objects.OrderItem
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentOrderDetailsBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OrderDetailsFragment : Fragment() {

    private lateinit var binding: FragmentOrderDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderDetailsBinding.inflate(inflater)

        val orderId = arguments?.getInt("order_id")
        if (orderId != null) {
            val orderList = ArrayList<OrderItem>()
            binding.recyclerProductsView.layoutManager = LinearLayoutManager(requireContext())
            val itemDecoration = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            binding.recyclerProductsView.addItemDecoration(itemDecoration)
            val orderAdapter = OrderlistAdapter(orderList)
            binding.recyclerProductsView.adapter = orderAdapter
            orderAdapter.setOnClickListener(object: OrderlistAdapter.OnClickListener{
                override fun onClick(item: OrderItem) {
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
                        .selectedItemId = R.id.navigation_home
                    val bundle = Bundle()
                    bundle.putString("searchQuery", item.name)
                    val productFragment = ProductFragment()
                    productFragment.arguments = bundle
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.home_fragment_home_container, productFragment,"ProductFragment")
                        .commit()
                    requireActivity().findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView).clearFocus()
                }
            })
            setOrderDetails(orderId)
            setOrderProducts(orderId,orderList)
        }

        binding.backButton.setOnClickListener{
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
            parentFragmentManager.findFragmentByTag("OrderHistoryFragment")?.let {
                parentFragmentManager.beginTransaction()
                    .show(it)
                    .commit()
            }
        }

        return binding.root
    }

    private fun setOrderProducts(orderId: Int,orderList: ArrayList<OrderItem>){
        val query = "SELECT p.name, oi.price, oi.quantity, pp.picture_path, p.price AS original_price " +
                "FROM orders o,order_items oi, product_pictures pp, products p " +
                "WHERE o.id = oi.order_id AND oi.color_id = pp.color_id AND pp.product_id = p.id and pp.picture_index = 0 " +
                "AND o.id = %s;"
        val params = com.google.gson.JsonArray().apply { add(orderId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val ordersArray = response.body()?.getAsJsonArray("queryset")
                    if(ordersArray != null && ordersArray.size()>0) {
                        var loadedOrders = 0
                        for (i in 0 until ordersArray.size()) {
                            val orderItemObject = ordersArray.get(i).asJsonObject
                            val name = orderItemObject.get("name").asString
                            val picture_path = orderItemObject.get("picture_path").asString
                            val price = orderItemObject.get("price").asDouble
                            val qty = orderItemObject.get("quantity").asInt
                            val originalPrice = orderItemObject.get("original_price").asDouble
                            ClientNetwork.retrofit.image(picture_path).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        if (response.body() != null) {
                                            val picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                            val orderItem = OrderItem(orderId, name, picture, price = price, quantity = qty, originalPrice = originalPrice)
                                            orderList.add(orderItem)
                                            loadedOrders++
                                            if (loadedOrders == ordersArray.size())
                                                binding.recyclerProductsView.adapter?.notifyDataSetChanged()
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

    private fun setOrderDetails(orderId: Int){
        val query = "SELECT o.order_date, o.total_price, ua.address_line1, ua.address_line2, ua.name, ua.city, ua.state, ua.postal_code, ua.county " +
                "FROM orders o JOIN user_addresses ua ON o.address_id = ua.id WHERE o.id = %s;"
        val params = com.google.gson.JsonArray().apply { add(orderId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val orderObject = response.body()?.getAsJsonArray("queryset")?.get(0)?.asJsonObject
                    if(orderObject != null) {
                        val date = orderObject.get("order_date").asString
                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        val orderDate = LocalDateTime.parse(date, formatter).toLocalDate()
                        val totalAmt = orderObject.get("total_price").asDouble
                        val addressLine1 = orderObject.get("address_line1").asString
                        val addressLine2 = orderObject.get("address_line2").asString
                        val name = orderObject.get("name").asString
                        val city = orderObject.get("city").asString
                        val state = orderObject.get("state").asString
                        val cap = orderObject.get("postal_code").asString
                        val county = orderObject.get("county").asString

                        binding.orderNumber.text = orderId.toString()
                        binding.orderDate.text = orderDate.toString()
                        binding.totalAmount.text = totalAmt.toString() + " €"
                        binding.addressLine1.text = addressLine1.toString()
                        binding.addressLine2.text = addressLine2.toString()
                        binding.addressName.text = name.toString()
                        binding.city.text = city.toString()
                        binding.state.text = state.toString()
                        binding.cap.text = cap.toString()
                        binding.county.text = county.toString()
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }
}
