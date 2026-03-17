package com.progetto.nomeprogetto.Activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.progetto.nomeprogetto.Adapters.AddressAdapter
import com.progetto.nomeprogetto.Adapters.CardAdapter
import com.progetto.nomeprogetto.Adapters.CartAdapter
import com.progetto.nomeprogetto.ClientNetwork
import com.progetto.nomeprogetto.Fragments.MainActivity.Account.AddAddressFragment
import com.progetto.nomeprogetto.Fragments.MainActivity.Account.AddCardFragment
import com.progetto.nomeprogetto.Fragments.MainActivity.Home.ProductDetailFragment
import com.progetto.nomeprogetto.Objects.Product
import com.progetto.nomeprogetto.Objects.UserAddress
import com.progetto.nomeprogetto.Objects.UserCard
import com.progetto.nomeprogetto.R
import com.progetto.nomeprogetto.databinding.ActivityBuyBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BuyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)

        binding.backButton.setOnClickListener{
            this.finish()
        }

        //gestione indirizzi:
        loadAddresses(userId)
        val addressSelectionLayout = binding.addressSelection
        val modifyAddressButton = addressSelectionLayout.modifyAddress
        val addAddressButton = addressSelectionLayout.addAddress
        modifyAddressButton.setOnClickListener{
            if(modifyAddressButton.text.equals(" Modifica")) {
                setToClose(modifyAddressButton)
                addressSelectionLayout.layoutIndirizzo.visibility = View.GONE
                addressSelectionLayout.addressListLayout.visibility = View.VISIBLE
                addressSelectionLayout.addressText.text = "Seleziona indirizzo di consegna"
            }else {
                setToOpen(modifyAddressButton)
                loadAddresses(userId)
                addressSelectionLayout.layoutIndirizzo.visibility = View.VISIBLE
                addressSelectionLayout.addressListLayout.visibility = View.GONE
                addressSelectionLayout.addressText.text = "1 Indirizzo di consegna"
            }
        }
        addAddressButton.setOnClickListener{
            supportFragmentManager.beginTransaction()
                .add(binding.fragmentContainer.id,AddAddressFragment())
                .commit()
            binding.fragmentContainer.visibility = View.VISIBLE
        }

        //gestione pagamenti
        loadCards(userId)
        val cardSelectionLayout = binding.cardSelection
        val modifyCardButton = cardSelectionLayout.modifyCard
        val addCardButton = cardSelectionLayout.addCard
        modifyCardButton.setOnClickListener {
            if (modifyCardButton.text.equals(" Modifica")) {
                setToClose(modifyCardButton)
                cardSelectionLayout.layoutCarta.visibility = View.GONE
                cardSelectionLayout.cardListLayout.visibility = View.VISIBLE
                cardSelectionLayout.cardText.text = "Seleziona metodo di pagamento"
            } else {
                setToOpen(modifyCardButton)
                loadCards(userId)
                cardSelectionLayout.layoutCarta.visibility = View.VISIBLE
                cardSelectionLayout.cardListLayout.visibility = View.GONE
                cardSelectionLayout.cardText.text = "2 Metodo di pagamento"
            }
        }
        addCardButton.setOnClickListener{
            supportFragmentManager.beginTransaction()
                .add(binding.fragmentContainer.id,AddCardFragment())
                .commit()
            binding.fragmentContainer.visibility = View.VISIBLE
        }

        val orderList = ArrayList<Product>()

        loadProducts(userId,orderList)

        binding.buyButton.setOnClickListener{
            var totalAmt = 0.0
            for (product in orderList)
                    totalAmt += product.discounted_price * (product.quantity ?: 0)
            createOrder(orderList, totalAmt, userId)
        }
    }

    private fun setToClose(button: TextView){
        button.text = " Chiudi"
        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close,0,0,0)
        button.compoundDrawableTintList = ContextCompat.getColorStateList(this,android.R.color.darker_gray)
    }

    private fun setToOpen(button: TextView){
        button.text = " Modifica"
        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_settings,0,0,0)
        button.compoundDrawableTintList = ContextCompat.getColorStateList(this,android.R.color.darker_gray)
    }

    private fun loadAddresses(userId: Int){
        val addressList = ArrayList<UserAddress>()
        getAddressId(0,userId) { selectedId ->
            setAddresses(addressList, userId, selectedId)
        }
        val recyclerView = binding.addressSelection.recyclerAddressView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AddressAdapter(addressList)
    }

    private fun loadCards(userId: Int){
        val cardList = ArrayList<UserCard>()
        getAddressId(1,userId) { selectedId ->
            setCards(cardList, userId, selectedId)
        }
        val recyclerView = binding.cardSelection.recyclerCardView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CardAdapter(cardList)
    }

    private fun loadProducts(userId: Int,orderList: ArrayList<Product>){
        setProducts(orderList,userId)

        val recyclerView = binding.orderSummary.recyclerOrderView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = CartAdapter(orderList,null,userId)
        recyclerView.adapter = adapter
        adapter.setOnClickListener(object: CartAdapter.OnClickListener{
            override fun onClick(product: Product){
                val bundle = Bundle()
                bundle.putParcelable("product", product)
                val productDetailFragment = ProductDetailFragment()
                productDetailFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .add(binding.fragmentContainer.id,productDetailFragment)
                    .commit()
                binding.fragmentContainer.visibility = View.GONE
            }
        })
    }

    private fun setAddresses(addressList: ArrayList<UserAddress>, userId: Int,selectedId:Int){
        val query = "SELECT id,address_line1,address_line2,name,city,county,state,postal_code " +
                "FROM user_addresses WHERE user_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        val addressSelectionLayout = binding.addressSelection
        var selected_id = selectedId

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    var loadedAddresses = 0
                    val addressArray = response.body()?.getAsJsonArray("queryset")
                    if (addressArray != null && addressArray.size() > 0) {
                        binding.addressSelection.addressListText.visibility = View.VISIBLE
                        binding.cardSelection.root.visibility = View.VISIBLE
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
                            if (selected_id==id)
                                setDefaultAddress(address)
                            loadedAddresses++
                            addressList.add(address)
                            if (loadedAddresses==addressArray.size())
                                addressSelectionLayout.recyclerAddressView.adapter?.notifyDataSetChanged()
                        }
                    }else{
                        addressSelectionLayout.addressListLayout.visibility = View.VISIBLE
                        addressSelectionLayout.addressListText.visibility = View.GONE
                        addressSelectionLayout.modifyAddress.visibility = View.GONE
                        addressSelectionLayout.addressText.text = "Aggiungi un indirizzo di consegna"
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@BuyActivity , "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setCards(cardList: ArrayList<UserCard>, userId: Int,selectedId:Int){
        val query = "SELECT id,cardholder_name,card_number,expiration_date,cvv " +
                "FROM user_payments WHERE user_id=%s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        val cardSelectionLayout = binding.cardSelection
        var selected_id = selectedId

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    var loadedCards = 0
                    val cardsArray = response.body()?.getAsJsonArray("queryset")
                    if (cardsArray != null && cardsArray.size() > 0) {
                        binding.cardSelection.cardListText.visibility = View.VISIBLE
                        binding.orderSummary.root.visibility = View.VISIBLE
                        binding.buyButton.visibility = View.VISIBLE
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
                            if (selected_id==id)
                                setDefaultCard(card)
                            loadedCards++
                            cardList.add(card)
                            if (loadedCards==cardsArray.size())
                                cardSelectionLayout.recyclerCardView.adapter?.notifyDataSetChanged()
                        }
                    }else{
                        cardSelectionLayout.cardListLayout.visibility = View.VISIBLE
                        cardSelectionLayout.cardListText.visibility = View.GONE
                        cardSelectionLayout.modifyCard.visibility = View.GONE
                        cardSelectionLayout.cardText.text = "Aggiungi una carta"
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@BuyActivity , "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setProducts(orderList: ArrayList<Product>, userId: Int){
        orderList.clear()
        val query = "SELECT ci.id AS itemId,ci.quantity,pc.stock,pc.color,pc.color_hex," +
                    "p.id,p.name,p.description,p.price,p.width,p.height,p.length," +
                    "p.main_picture_path,p.upload_date,pp.picture_path,ci.color_id," +
                    "IFNULL((SELECT COUNT(*) FROM product_reviews WHERE product_id = p.id),0) AS review_count," +
                    "IFNULL((SELECT AVG(rating) FROM product_reviews WHERE product_id = p.id),0) AS avg_rating," +
                    "s.discount, ROUND(p.price * (1 - IFNULL(s.discount,0) / 100.0), 2) AS discounted_price " +
                    "FROM cart_items ci " +
                    "JOIN products p ON p.id = ci.product_id " +
                    "JOIN product_colors pc ON pc.id = ci.color_id " +
                    "JOIN product_pictures pp ON pp.color_id = pc.id AND pp.picture_index = 0 AND pp.product_id = p.id " +
                    "LEFT JOIN sales s ON s.product_id = p.id AND NOW() BETWEEN s.start_date AND s.end_date " +
                    "WHERE ci.user_id = %s;"
        val params = com.google.gson.JsonArray().apply { add(userId) }.toString()

        ClientNetwork.retrofit.selectSafe(query, params).enqueue(object : Callback<JsonObject> {
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
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            val uploadDate = LocalDateTime.parse(date, formatter)
                            val main_picture_path = productObject.get("main_picture_path").asString
                            val color = productObject.get("color").asString
                            val itemId = productObject.get("itemId").asInt
                            val color_hex = productObject.get("color_hex").asString
                            val picture_path = productObject.get("picture_path").asString
                            val colorId = productObject.get("color_id").asInt
                            val discountElem = productObject.get("discount")
                            val discount = if (discountElem == null || discountElem.isJsonNull) null else discountElem.asInt
                            val discountedPrice = productObject.get("discounted_price").asDouble
                            var stock = productObject.get("stock").asInt
                            var quantity = productObject.get("quantity").asInt
                            if (stock>0 && quantity>stock){
                                quantity = stock
                                val updateQuery = "UPDATE cart_items set quantity=%s where id=%s;"
                                val updateParams = com.google.gson.JsonArray().apply { add(quantity); add(itemId) }.toString()
                                ClientNetwork.retrofit.updateSafe(updateQuery, updateParams).enqueue(object : Callback<JsonObject> {
                                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {}
                                    override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                        Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                                })
                            }
                            var main_picture : Bitmap? = null
                            var picture : Bitmap? = null
                            for(j in 0..1) {
                                ClientNetwork.retrofit.image(if (j==0) main_picture_path else picture_path).enqueue(object : Callback<ResponseBody> {
                                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                        if (response.isSuccessful) {
                                            if (response.body() != null) {
                                                if(j==0) main_picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                                else picture = BitmapFactory.decodeStream(response.body()?.byteStream())
                                                if(j==1) {
                                                    val product = Product(id, name, description, price, width, height,
                                                        length, main_picture, avgRating, reviewsNumber, uploadDate,
                                                        itemId, color, color_hex, quantity, stock, picture, colorId,
                                                        discount = discount, discounted_price = discountedPrice)
                                                    if (product.stock != null && product.stock>0)
                                                        orderList.add(product)
                                                    loadedProducts++
                                                    if (loadedProducts == productsArray.size()) {
                                                        binding.orderSummary.recyclerOrderView.adapter?.notifyDataSetChanged()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) =
                                        Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                                })
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun setDefaultAddress(address: UserAddress){
        val addressSelection = binding.addressSelection
        addressSelection.layoutIndirizzo.visibility = View.VISIBLE
        addressSelection.name.text = address.name
        addressSelection.addressLine1.text = address.address_line1
        addressSelection.addressLine2.text = address.address_line2
        addressSelection.city.text = address.city
        addressSelection.county.text = address.county
        addressSelection.cap.text = address.cap
        addressSelection.state.text = address.state
    }

    private fun setDefaultCard(card: UserCard){
        val cardSelection = binding.cardSelection
        cardSelection.layoutCarta.visibility = View.VISIBLE
        cardSelection.cardholderName.text = card.name
        cardSelection.cardNumber.text = card.card_number
        cardSelection.expirationDate.text = card.expiration_date
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
                Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun createOrder(orderList: ArrayList<Product>,totalAmt: Double, userId: Int){
        val insertOrderQuery = "INSERT INTO orders (user_id,total_price,address_id) VALUES (%s,%s," +
                "(SELECT current_address_id FROM users WHERE id = %s));"
        val insertOrderParams = com.google.gson.JsonArray().apply { add(userId); add(totalAmt); add(userId) }.toString()

        ClientNetwork.retrofit.insertSafe(insertOrderQuery, insertOrderParams).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val selectLastOrderQuery = "SELECT id from orders ORDER BY order_date DESC LIMIT 1"
                    ClientNetwork.retrofit.select(selectLastOrderQuery).enqueue(object : Callback<JsonObject> {
                        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                            if (response.isSuccessful) {
                                val id = response.body()?.getAsJsonArray("queryset")?.get(0)?.asJsonObject
                                    ?.get("id")?.asInt
                                if (id == null)
                                    Toast.makeText(this@BuyActivity, "Errore nell'inserimento dell'ordine, riprova", Toast.LENGTH_LONG).show()
                                else{
                                    var loadedProducts = 0
                                    for(product in orderList){
                                        val itemQuery = "INSERT INTO order_items (order_id,product_id,color_id,quantity,price) " +
                                                "VALUES (%s,%s,%s,%s,%s);"
                                        val itemParams = com.google.gson.JsonArray().apply {
                                            add(id); add(product.id); add(product.colorId); add(product.quantity); add(product.discounted_price)
                                        }.toString()

                                        ClientNetwork.retrofit.insertSafe(itemQuery, itemParams).enqueue(object : Callback<JsonObject> {
                                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                                if (response.isSuccessful){
                                                    loadedProducts++
                                                    if (loadedProducts==orderList.size) {
                                                        removeFromCart(orderList)
                                                        Toast.makeText(this@BuyActivity, "Acquisto effettuato con successo", Toast.LENGTH_LONG).show()
                                                        this@BuyActivity.finish()
                                                    }
                                                }
                                            }
                                            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                                Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                                        })
                                    }
                                }
                            }
                        }
                        override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                            Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                    })
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun removeFromCart(orderList: ArrayList<Product>){
        for(product in orderList){
            val delQuery = "DELETE FROM cart_items WHERE id = %s;"
            val delParams = com.google.gson.JsonArray().apply { add(product.itemId) }.toString()

            ClientNetwork.retrofit.removeSafe(delQuery, delParams).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful){
                        val quantity = product.quantity
                        val newStock = if (quantity != null) (product.stock ?: 0) - quantity else 0
                        val stockQuery = "UPDATE product_colors SET stock = %s WHERE id = %s;"
                        val stockParams = com.google.gson.JsonArray().apply { add(newStock); add(product.colorId) }.toString()
                        ClientNetwork.retrofit.updateSafe(stockQuery, stockParams).enqueue(object : Callback<JsonObject> {
                            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {}
                            override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                                Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
                        })
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) =
                    Toast.makeText(this@BuyActivity, "Failed request: " + t.message, Toast.LENGTH_LONG).show()
            })
        }
    }

    fun onAddAddressFragmentClose(){
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        binding.fragmentContainer.visibility = View.GONE
        loadAddresses(userId)
        //caso in cui si aggiunge il primo indirizzo da qui:
        setToClose(binding.addressSelection.modifyAddress)
        binding.addressSelection.modifyAddress.visibility = View.VISIBLE
    }

    fun onAddCardFragmentClose(){
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("ID", 0)
        binding.fragmentContainer.visibility = View.GONE
        loadCards(userId)
        //caso in cui si aggiunge la prima carta da qui:
        setToClose(binding.cardSelection.modifyCard)
        binding.cardSelection.modifyCard.visibility = View.VISIBLE
    }
}