package com.iamashad.meraki.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides app-wide dependencies such as
 * FirebaseAuth and GoogleSignInClient, scoped to the application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of FirebaseAuth used for user authentication.
     *
     * @return FirebaseAuth instance.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provides a singleton instance of GoogleSignInClient configured with
     * the app's OAuth client ID and email request.
     *
     * @param context Application context provided by Hilt.
     * @return GoogleSignInClient instance.
     */
    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID) // Uses token from app's build config
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, googleSignInOptions)
    }
}
