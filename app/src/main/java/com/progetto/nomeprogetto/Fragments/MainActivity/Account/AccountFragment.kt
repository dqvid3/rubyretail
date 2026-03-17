package com.progetto.nomeprogetto.Fragments.MainActivity.Account

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentAccountBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater)

        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()
        ClientNetwork.retrofit.selectSafe("SELECT name, surname, email, username FROM users WHERE id = %s", params)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        val row = (response.body()?.get("queryset") as? JsonArray)?.firstOrNull()?.asJsonObject
                        if (row != null) {
                            binding.textViewNome.text = " Nome: ${row.get("name").asString}"
                            binding.textViewCognome.text = " Cognome: ${row.get("surname").asString}"
                            binding.textViewCognome2.text = " Email: ${row.get("email").asString}"
                            binding.emailTextView.text = " Username: ${row.get("username").asString}"
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                }
            })

        binding.addPayment.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.home_fragment_container, AddCardFragment())
                .hide(this)
                .commit()
        }

        binding.addAddress.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.home_fragment_container, AddAddressFragment())
                .hide(this)
                .commit()
        }

        binding.orderHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.home_fragment_container, OrderHistoryFragment(), "OrderHistoryFragment")
                .hide(this)
                .commit()
        }
        binding.modifyData.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.home_fragment_container, ModifyDataFragment())
                .hide(this)
                .commit()
        }



        return binding.root
    }
}
