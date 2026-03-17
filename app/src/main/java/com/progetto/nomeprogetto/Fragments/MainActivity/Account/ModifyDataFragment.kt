package com.progetto.nomeprogetto.Fragments.MainActivity.Account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ModifyDataFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_modify_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_name)
        val editSurname = view.findViewById<TextInputEditText>(R.id.edit_surname)
        val editUsername = view.findViewById<TextInputEditText>(R.id.edit_username)
        val editEmail = view.findViewById<TextInputEditText>(R.id.edit_email)

        val params = JsonArray().apply { add(userId) }.toString()
        ClientNetwork.retrofit.selectSafe("SELECT name, surname, email, username FROM users WHERE id = %s", params)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        val row = response.body()?.getAsJsonArray("queryset")?.firstOrNull()?.asJsonObject
                        if (row != null) {
                            editName.setText(row.get("name").asString)
                            editSurname.setText(row.get("surname").asString)
                            editUsername.setText(row.get("username").asString)
                            editEmail.setText(row.get("email").asString)
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                }
            })

        view.findViewById<FloatingActionButton>(R.id.back_button).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
            parentFragmentManager.findFragmentByTag("AccountFragment")?.let {
                parentFragmentManager.beginTransaction().show(it).commit()
            }
        }

        view.findViewById<Button>(R.id.save_button).setOnClickListener {
            val name = editName.text.toString().trim()
            val surname = editSurname.text.toString().trim()
            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()

            if (name.isBlank() || surname.isBlank() || username.isBlank() || email.isBlank()) {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateQuery = "UPDATE users SET name=%s, surname=%s, username=%s, email=%s WHERE id=%s;"
            val updateParams = JsonArray().apply {
                add(name); add(surname); add(username); add(email); add(userId)
            }.toString()

            ClientNetwork.retrofit.updateSafe(updateQuery, updateParams)
                .enqueue(object : Callback<JsonObject> {
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        if (response.isSuccessful)
                            Toast.makeText(requireContext(), "Dati aggiornati con successo", Toast.LENGTH_SHORT).show()
                        else
                            Toast.makeText(requireContext(), "Errore nell'aggiornamento, riprova", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                        Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
}
