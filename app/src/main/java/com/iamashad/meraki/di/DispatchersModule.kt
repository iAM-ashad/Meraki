package com.iamashad.meraki.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

// ---------------------------------------------------------------------------
// Qualifier annotations
// Distinguish between CoroutineDispatcher instances at injection sites.
// Usage: @IoDispatcher val ioDispatcher: CoroutineDispatcher
// ---------------------------------------------------------------------------

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

// ---------------------------------------------------------------------------
// Hilt module
// Binds each qualifier to the corresponding Kotlin Dispatchers singleton.
// Injecting the dispatcher (rather than referencing Dispatchers.IO directly)
// makes repositories and ViewModels testable with TestCoroutineDispatcher.
// ---------------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
