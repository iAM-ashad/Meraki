package com.iamashad.meraki.repository

import com.iamashad.meraki.model.Advice
import com.iamashad.meraki.network.AdviceApi
import javax.inject.Inject

class AdviceRepository @Inject constructor(
    private val adviceApi: AdviceApi
) {
    suspend fun getAdvice(): Advice {
        return adviceApi.getRandomAdvice()
    }
}

