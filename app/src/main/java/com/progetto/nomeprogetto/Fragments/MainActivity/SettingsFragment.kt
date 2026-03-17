package com.progetto.nomeprogetto.Fragments.MainActivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.progetto.nomeprogetto.Activities.LoginRegisterActivity
import com.progetto.nomeprogetto.Fragments.MainActivity.Settings.AddressAndCardFragment
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater)

        binding.logoutButton.setOnClickListener{
            val sharedPref = requireContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("IS_LOGGED_IN", false).putInt("ID",-1)
            editor.apply()
            val i = Intent(requireContext(), LoginRegisterActivity::class.java)
            startActivity(i)

            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()

            requireActivity().finish()
        }

        binding.yourAddresses.setOnClickListener{
            val fragment = AddressAndCardFragment()
            val bundle = Bundle()
            bundle.putInt("type",0)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add(R.id.home_fragment_container,fragment)
                .commit()
        }

        binding.yourCards.setOnClickListener{
            val fragment = AddressAndCardFragment()
            val bundle = Bundle()
            bundle.putInt("type",1)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add(R.id.home_fragment_container,fragment)
                .commit()
        }

        return binding.root
    }
}
