package com.progetto.nomeprogetto.Fragments.MainActivity.Account

import android.animation.Animator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Activities.BuyActivity
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Objects.UserCard
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.FragmentAddCardBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class AddCardFragment : Fragment() {

    private lateinit var binding: FragmentAddCardBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddCardBinding.inflate(inflater)

        val card: UserCard? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("card", UserCard::class.java)
        } else
            arguments?.getParcelable("card")

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val months = arrayOf("01","02","03","04","05","06","07","08","09","10","11","12")
        val years = Array(21) { (currentYear + it).toString() }

        var modify = false
        var cardMonth = ""
        var cardYear = ""

        if (card != null){
            binding.cardholderName.setText(card.name)
            binding.cardNumber.setText(card.card_number)
            val date = card.expiration_date.split("/")
            cardMonth = date[0]
            months.indexOf(cardMonth).takeIf { it != -1 }?.let { binding.month.setSelection(it) }
            cardYear = date[1]
            years.indexOf(cardYear).takeIf { it != -1 }?.let { binding.year.setSelection(it) }
            modify = true
        }

        binding.month.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.year.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.saveButton.setOnClickListener {
            val name = binding.cardholderName.text.toString()
            val card_number = binding.cardNumber.text.toString()
            val month = binding.month.selectedItem
            val year = binding.year.selectedItem
            val cvv = binding.cvv.text.toString().trim()
            if (name.isEmpty() || card_number.isEmpty() || cvv.length != 3) {
                Toast.makeText(requireContext(), "Perfavore riempi tutti i campi correttamente", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                val userId = sharedPref.getInt("ID", 0)
                val insertedCard = UserCard(0, name, card_number, "$month/$year", cvv.toInt())
                if (modify) {
                    if (card != null) {
                        if (card.name == name && card.card_number == card_number && cardMonth == month && cardYear == year && cvv.isBlank())
                            Toast.makeText(requireContext(), "Non hai modificato nulla", Toast.LENGTH_SHORT).show()
                        else {
                            insertedCard.id = card.id
                            updateCard(insertedCard)
                        }
                    }
                } else {
                    cardExists(insertedCard, userId) { exists ->
                        if (exists)
                            Toast.makeText(requireContext(), "Carta già esistente nel tuo account", Toast.LENGTH_SHORT).show()
                        else
                            addCard(insertedCard, userId)
                    }
                }
            }
        }

        binding.animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {}
            override fun onAnimationEnd(p0: Animator) { closeFragment() }
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationRepeat(p0: Animator) {}
        })

        binding.backButton.setOnClickListener{ closeFragment() }

        return binding.root
    }

    private fun cardExists(card: UserCard, userId: Int, callback: (Boolean) -> Unit){
        val query = "SELECT id FROM user_payments WHERE user_id = %s AND card_number = %s;"
        val params = JsonArray().apply { add(userId); add(card.card_number) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful)
                    callback((response.body()?.get("queryset") as JsonArray).size() == 1)
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun addCard(c: UserCard, userId: Int) {
        val query = "INSERT INTO user_payments (user_id, card_number, cardholder_name, cvv, expiration_date) " +
                "VALUES (%s, %s, %s, %s, %s);"
        val params = JsonArray().apply {
            add(userId); add(c.card_number); add(c.name); add(c.cvv); add(c.expiration_date)
        }.toString()

        ClientNetwork.retrofit.insertSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if(response.isSuccessful) { Toast.makeText(requireContext(), "Carta salvata con successo", Toast.LENGTH_SHORT).show(); startAnimation() }
                else Toast.makeText(requireContext(), "Errore nel salvataggio, riprova", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun updateCard(c: UserCard) {
        val query = "UPDATE user_payments SET card_number = %s, cardholder_name = %s, " +
                "expiration_date = %s, cvv = %s WHERE id = %s;"
        val params = JsonArray().apply {
            add(c.card_number); add(c.name); add(c.expiration_date); add(c.cvv); add(c.id)
        }.toString()

        ClientNetwork.retrofit.updateSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if(response.isSuccessful) { Toast.makeText(requireContext(), "Carta salvata con successo", Toast.LENGTH_SHORT).show(); startAnimation() }
                else Toast.makeText(requireContext(), "Errore nel salvataggio, riprova", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(requireContext(), "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun startAnimation(){
        val checkAnimationView: LottieAnimationView = binding.animationView
        binding.animationView.speed = 3.0f
        checkAnimationView.playAnimation()
    }

    private fun closeFragment(){
        parentFragmentManager.beginTransaction().remove(this).commit()
        parentFragmentManager.findFragmentByTag("AccountFragment")?.let {
            parentFragmentManager.beginTransaction().show(it).commit()
        }
        (activity as? BuyActivity)?.onAddCardFragmentClose()
    }
}
