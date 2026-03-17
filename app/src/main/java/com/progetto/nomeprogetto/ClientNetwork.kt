package com.progetto.nomeprogetto

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ClientNetwork {
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InterfaceAPI::class.java)
    }
}