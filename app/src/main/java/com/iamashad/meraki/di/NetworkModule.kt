package com.iamashad.meraki.di

import com.iamashad.meraki.network.AdviceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This ensures the scope is Singleton
object NetworkModule {

    @Provides
    @Singleton // Ensure Retrofit is also Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.adviceslip.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton // Ensure AdviceApi is Singleton too
    fun provideAdviceApi(retrofit: Retrofit): AdviceApi {
        return retrofit.create(AdviceApi::class.java)
    }
}
