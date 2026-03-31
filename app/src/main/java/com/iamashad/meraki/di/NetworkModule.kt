package com.iamashad.meraki.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.BuildConfig
import com.iamashad.meraki.network.GroqApiService
import com.iamashad.meraki.network.QuotesAPI
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.repository.MoodRepository
import com.iamashad.meraki.utils.ConnectivityStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
     * Qualifier annotation for the Groq Cloud Retrofit instance.
     * Prevents Hilt from confusing it with the [QuotesRetrofit] instance.
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class GroqRetrofit

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
     * Phase 3: @IoDispatcher injected so all Firestore operations and mapping run on IO thread.
     */
    @Provides
    @Singleton
    fun provideFirestoreRepository(
        db: FirebaseFirestore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FirestoreRepository {
        return FirestoreRepository(db, ioDispatcher)
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
     * Phase 3: @IoDispatcher injected so addMood() and getAllMoods() run on IO thread.
     */
    @Provides
    @Singleton
    fun provideMoodRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MoodRepository {
        return MoodRepository(firestore, auth, ioDispatcher)
    }

    // -----------------------------------------------------------------------
    // Groq Cloud — network infrastructure (Step 1)
    // -----------------------------------------------------------------------

    /**
     * Provides the OkHttpClient used exclusively for Groq API calls.
     *
     * Responsibilities:
     *  - Adds a `User-Agent` header identifying the app.
     *  - Attaches HEADERS-level logging in all build variants so request/response
     *    details are visible in Logcat during development (no body logging to avoid
     *    leaking prompt content in logs).
     *
     * Note: The `Authorization` header is NOT added here — it is injected
     * per-call via [GroqApiService]'s `@Header` parameter so individual requests
     * can use different keys if needed in the future.
     */
    @Provides
    @Singleton
    fun provideGroqOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Meraki-Android/${BuildConfig.VERSION_NAME}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    /**
     * Provides a singleton Retrofit instance pointed at the Groq Cloud base URL.
     *
     * Uses Gson for serialisation so it is consistent with the rest of the app's
     * network layer.  The [OkHttpClient] wires in the User-Agent interceptor.
     */
    @GroqRetrofit
    @Provides
    @Singleton
    fun provideGroqRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(GROQ_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Provides a singleton [GroqApiService] created from the [GroqRetrofit] instance.
     */
    @Provides
    @Singleton
    fun provideGroqApiService(@GroqRetrofit retrofit: Retrofit): GroqApiService =
        retrofit.create(GroqApiService::class.java)
}

/**
 * Base URL used for accessing the Quotes API service.
 */
const val QUOTES_API_BASE_URL = "https://zenquotes.io/"

/**
 * Base URL for the Groq Cloud API.
 * All endpoints are appended relative to this root (e.g. "openai/v1/chat/completions").
 */
const val GROQ_BASE_URL = "https://api.groq.com/"
