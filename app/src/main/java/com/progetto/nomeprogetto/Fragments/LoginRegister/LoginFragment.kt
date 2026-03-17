package com.progetto.nomeprogetto.Fragments.LoginRegister

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Activities.MainActivity
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater)

        binding.loginButton.setOnClickListener{
            loginUser(binding.editTextEmail.text.toString(),binding.editTextPassword.text.toString())
        }

        binding.registerTextView.setOnClickListener{
            openFragment(RegisterFragment())
        }

        return binding.root
    }

    private fun loginUser(email: String, password: String){
        ClientNetwork.retrofit.login(email, password).enqueue(
            object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        val user = response.body()?.get("queryset") as JsonArray
                        if (user.size() == 1) {
                            val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                            sharedPref.edit().putBoolean("IS_LOGGED_IN", true)
                                .putInt("ID", user[0].asJsonObject.get("id").asInt)
                                .apply()
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        } else {
                            Toast.makeText(requireContext(), "Credenziali errate", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed to login: " + t.message, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun openFragment(fragment: Fragment){
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_login_container,fragment)
            .commit()
    }
}