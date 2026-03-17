package com.progetto.nomeprogetto.Fragments.MainActivity.Account

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Adapters.OrderlistAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Objects.OrderItem
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentOrderHistoryBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OrderHistoryFragment : Fragment() {

    private lateinit var binding: FragmentOrderHistoryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderHistoryBinding.inflate(inflater)

        binding.recyclerOrderView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        binding.recyclerOrderView.addItemDecoration(itemDecoration)
        val orderList = ArrayList<OrderItem>()
        val orderAdapter = OrderlistAdapter(orderList)
        binding.recyclerOrderView.adapter = orderAdapter

        orderAdapter.setOnClickListener(object : OrderlistAdapter.OnClickListener {
            override fun onClick(item: OrderItem) {
                val fragment = OrderDetailsFragment()
                val bundle = Bundle()
                bundle.putInt("order_id",item.orderId)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .add(R.id.home_fragment_container,fragment)
                    .hide(this@OrderHistoryFragment)
                    .commit()
            }
        })

        setOrderHistory(orderList)

        binding.backButton.setOnClickListener{
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
            parentFragmentManager.findFragmentByTag("AccountFragment")?.let {
                parentFragmentManager.beginTransaction()
                    .show(it)
                    .commit()
            }
        }

        return binding.root
    }

    private fun setOrderHistory(orderList: ArrayList<OrderItem>){
        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        val query = "SELECT oi.order_id, p.name, pp.picture_path, o.order_date " +
                "FROM orders o,order_items oi, product_pictures pp, products p " +
                "WHERE o.id = oi.order_id AND oi.color_id = pp.color_id AND pp.product_id = p.id " +
                "AND o.user_id = %s AND pp.picture_index=0 ORDER BY o.order_date DESC, oi.order_id DESC;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val ordersArray = response.body()?.getAsJsonArray("queryset")
                    if(ordersArray != null && ordersArray.size()>0) {
                        var loadedOrders = 0
                        for (i in 0 until ordersArray.size()) {
                            val orderItemObject = ordersArray.get(i).asJsonObject
                            val orderId = orderItemObject.get("order_id").asInt
                            val name = orderItemObject.get("name").asString
                            val picture_path = orderItemObject.get("picture_path").asString
                            val date = orderItemObject.get("order_date").asString
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            val orderDate = LocalDateTime.parse(date, formatter).toLocalDate()
                            ClientNetwork.retrofit.image(picture_path).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        if (response.body() != null) {
                                            val picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                            val orderItem = OrderItem(orderId,name,picture,orderDate)
                                            orderList.add(orderItem)
                                            loadedOrders++
                                            if (loadedOrders == ordersArray.size())
                                                binding.recyclerOrderView.adapter?.notifyDataSetChanged()
                                        }
                                    }
                                }
                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) =
                                    Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                            })
                        }
                    }else
                        binding.emptyOrders.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }
}
