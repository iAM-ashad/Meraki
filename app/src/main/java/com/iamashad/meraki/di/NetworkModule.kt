package com.iamashad.meraki.di

import com.iamashad.meraki.network.AdviceApi
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.repository.MoodRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AdviceRetrofit

    @AdviceRetrofit
    @Provides
    @Singleton
    fun provideAdviceRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.adviceslip.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAdviceApi(@AdviceRetrofit retrofit: Retrofit): AdviceApi =
        retrofit.create(AdviceApi::class.java)

    @Provides
    @Singleton
    fun provideFirestoreRepository(): FirestoreRepository {
        return FirestoreRepository()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideMoodRepository(firestore: FirebaseFirestore): MoodRepository {
        return MoodRepository(firestore)
    }
}
