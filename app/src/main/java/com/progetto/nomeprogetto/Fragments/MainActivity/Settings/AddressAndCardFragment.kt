package com.progetto.nomeprogetto.Fragments.MainActivity.Settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Adapters.AddressAdapter
import com.progetto.nomeprogetto.Adapters.CardAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Objects.UserAddress
import com.progetto.nomeprogetto.Objects.UserCard
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentAddressAndCardBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddressAndCardFragment : Fragment() {

    private lateinit var binding: FragmentAddressAndCardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddressAndCardBinding.inflate(inflater)

        binding.backButton.setOnClickListener{
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
            parentFragmentManager.findFragmentByTag("SettingsFragment")?.let {
                parentFragmentManager.beginTransaction()
                    .show(it)
                    .commit()
            }
        }

        val type = arguments?.getInt("type")

        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)

        if (type==0) {
            binding.viewText.text = "I tuoi indirizzi"
            binding.emptyText.text = "Non hai inserito nessun indirizzo\nProvvedi nella sezione account"
            loadAddresses(userId)
        }else {
            binding.viewText.text = "Le tue carte"
            binding.emptyText.text = "Non hai inserito nessuna carta\nProvvedi nella sezione account"
            loadCards(userId)
        }

        return binding.root
    }

    private fun loadAddresses(userId: Int){
        val addressList = ArrayList<UserAddress>()
        getAddressId(0,userId) { selectedId ->
            setAddresses(addressList, userId, selectedId)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = AddressAdapter(addressList)
    }

    private fun loadCards(userId: Int){
        val cardList = ArrayList<UserCard>()
        getAddressId(1,userId) { selectedId ->
            setCards(cardList, userId, selectedId)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = CardAdapter(cardList)
    }

    private fun setAddresses(addressList: ArrayList<UserAddress>, userId: Int, selectedId:Int){
        addressList.clear()
        val query = "SELECT id,address_line1,address_line2,name,city,county,state,postal_code " +
                "FROM user_addresses WHERE user_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        var selected_id = selectedId

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    var loadedAddresses = 0
                    val addressArray = response.body()?.getAsJsonArray("queryset")
                    if (addressArray != null && addressArray.size() > 0) {
                        for (i in 0 until addressArray.size()) {
                            val addressObject = addressArray[i].asJsonObject
                            val id = addressObject.get("id").asInt
                            val address_line1 = addressObject.get("address_line1").asString
                            val address_line2 = addressObject.get("address_line2").asString
                            val name = addressObject.get("name").asString
                            val city = addressObject.get("city").asString
                            val county = addressObject.get("county").asString
                            val postal_code = addressObject.get("postal_code").asString
                            val state = addressObject.get("state").asString
                            if (i==0 && selected_id==-1)
                                selected_id = id
                            val address = UserAddress(id,name,state,address_line1,address_line2,postal_code,city,county,
                                selected_id==id)
                            loadedAddresses++
                            addressList.add(address)
                            if (loadedAddresses==addressArray.size())
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                        }
                    }else
                        binding.emptyText.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext() , "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setCards(cardList: ArrayList<UserCard>, userId: Int,selectedId:Int){
        cardList.clear()
        val query = "SELECT id,cardholder_name,card_number,expiration_date,cvv " +
                "FROM user_payments WHERE user_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        var selected_id = selectedId

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    var loadedCards = 0
                    val cardsArray = response.body()?.getAsJsonArray("queryset")
                    if (cardsArray != null && cardsArray.size() > 0) {
                        for (i in 0 until cardsArray.size()) {
                            val cardObject = cardsArray[i].asJsonObject
                            val id = cardObject.get("id").asInt
                            val cardholder_name = cardObject.get("cardholder_name").asString
                            val card_number = cardObject.get("card_number").asString
                            val expiration_date = cardObject.get("expiration_date").asString
                            val cvv = cardObject.get("cvv").asInt
                            if (i==0 && selected_id==-1)
                                selected_id = id
                            val card = UserCard(id,cardholder_name,card_number,expiration_date,cvv,selected_id==id)
                            loadedCards++
                            cardList.add(card)
                            if (loadedCards==cardsArray.size())
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                        }
                    }else
                        binding.emptyText.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext() , "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getAddressId(type: Int,userId: Int, callback: (Int) -> Unit) {
        val query = if (type==0)
            "SELECT current_address_id as current_id FROM users WHERE id=%s;"
        else
            "SELECT current_card_id as current_id FROM users WHERE id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val resultSet = response.body()?.getAsJsonArray("queryset")
                    if (resultSet != null && resultSet.size() > 0) {
                        val currentId = resultSet[0].asJsonObject.get("current_id").asInt
                        callback.invoke(currentId)
                    } else
                        callback.invoke(-1) // No current address
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }
}
