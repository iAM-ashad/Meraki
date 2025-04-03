package com.iamashad.meraki.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.BuildConfig
import com.iamashad.meraki.network.QuotesAPI
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.repository.MoodRepository
import com.iamashad.meraki.utils.ConnectivityStatus
import com.iamashad.meraki.utils.provGenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module that provides network, Firestore, AI, and utility dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Qualifier annotation for distinguishing the Retrofit instance used for Quotes API.
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class QuotesRetrofit

    /**
     * Provides a singleton Retrofit instance configured with the Quotes API base URL.
     */
    @QuotesRetrofit
    @Provides
    @Singleton
    fun provideQuotesRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(QUOTES_API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Provides a QuotesAPI instance created using the qualified Retrofit builder.
     */
    @Provides
    @Singleton
    fun provideQuotesApi(@QuotesRetrofit retrofit: Retrofit): QuotesAPI =
        retrofit.create(QuotesAPI::class.java)

    /**
     * Provides a singleton instance of ConnectivityStatus to observe network status.
     */
    @Provides
    @Singleton
    fun provideConnectivityStatus(@ApplicationContext context: Context): ConnectivityStatus {
        return ConnectivityStatus(context)
    }

    /**
     * Provides a singleton instance of FirestoreRepository for managing Firestore operations.
     */
    @Provides
    @Singleton
    fun provideFirestoreRepository(db: FirebaseFirestore): FirestoreRepository {
        return FirestoreRepository(db)
    }

    /**
     * Provides a singleton instance of FirebaseFirestore.
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /**
     * Provides a singleton instance of MoodRepository to handle mood-related Firestore data.
     */
    @Provides
    @Singleton
    fun provideMoodRepository(firestore: FirebaseFirestore, auth: FirebaseAuth): MoodRepository {
        return MoodRepository(firestore, auth)
    }

    /**
     * Provides an instance of GenerativeModel configured with Gemini API key.
     */
    @Provides
    fun provideGenerativeModel(): GenerativeModel {
        return provGenerativeModel(apiKey = BuildConfig.GEMINI_API_KEY)
    }
}

/**
 * Base URL used for accessing the Quotes API service.
 */
const val QUOTES_API_BASE_URL = "https://quotes-api-self.vercel.app/"
