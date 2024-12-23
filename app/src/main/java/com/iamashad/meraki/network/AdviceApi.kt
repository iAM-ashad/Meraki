package com.iamashad.meraki.network

import com.iamashad.meraki.model.Advice
import retrofit2.http.GET

interface AdviceApi {

    @GET("advice")
    suspend fun getRandomAdvice(): Advice
}