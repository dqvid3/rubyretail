package com.progetto.nomeprogetto

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface InterfaceAPI {

    // Raw query endpoints — use only for queries with no user-supplied values
    @POST("postSelect/")
    @FormUrlEncoded
    fun select(@Field("query") query: String): Call<JsonObject>

    @POST("postInsert/")
    @FormUrlEncoded
    fun insert(@Field("query") query: String): Call<JsonObject>

    @POST("postRemove/")
    @FormUrlEncoded
    fun remove(@Field("query") query: String): Call<JsonObject>

    @POST("postUpdate/")
    @FormUrlEncoded
    fun update(@Field("query") query: String): Call<JsonObject>

    // Parameterized variants — use whenever query contains user input.
    // Pass query with %s placeholders, params as a JSON array string e.g. "[\"foo\", 42]"
    @POST("postSelect/")
    @FormUrlEncoded
    fun selectSafe(@Field("query") query: String, @Field("params") params: String): Call<JsonObject>

    @POST("postInsert/")
    @FormUrlEncoded
    fun insertSafe(@Field("query") query: String, @Field("params") params: String): Call<JsonObject>

    @POST("postRemove/")
    @FormUrlEncoded
    fun removeSafe(@Field("query") query: String, @Field("params") params: String): Call<JsonObject>

    @POST("postUpdate/")
    @FormUrlEncoded
    fun updateSafe(@Field("query") query: String, @Field("params") params: String): Call<JsonObject>

    // Auth endpoints — handle password hashing server-side
    @POST("login/")
    @FormUrlEncoded
    fun login(@Field("email") email: String, @Field("password") password: String): Call<JsonObject>

    @POST("register/")
    @FormUrlEncoded
    fun register(
        @Field("username") username: String,
        @Field("name") name: String,
        @Field("surname") surname: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<JsonObject>

    @GET
    fun image(@Url url: String): Call<ResponseBody>

}
