package com.iamashad.meraki.di

import com.iamashad.meraki.repository.GroqRepository
import com.iamashad.meraki.repository.GroqRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their concrete implementations.
 *
 * Using `@Binds` (abstract function) instead of `@Provides` avoids creating an
 * extra wrapper object at runtime — Hilt simply tells the DI graph that whenever
 * a [GroqRepository] is requested it should supply the already-constructed
 * [GroqRepositoryImpl] singleton.
 *
 * All repository bindings that follow the same interface → implementation pattern
 * belong here, keeping [NetworkModule] focused on network infrastructure only.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [GroqRepositoryImpl] as the singleton provider of [GroqRepository].
     *
     * Hilt constructs [GroqRepositoryImpl] by injecting:
     *  - [com.iamashad.meraki.network.GroqApiService]  (from [NetworkModule])
     *  - [@IoDispatcher][IoDispatcher] [kotlinx.coroutines.CoroutineDispatcher]
     *    (from [DispatchersModule])
     *
     * Both dependencies are already registered as singletons, so the bound
     * repository is effectively a singleton for the lifetime of the application.
     */
    @Binds
    @Singleton
    abstract fun bindGroqRepository(impl: GroqRepositoryImpl): GroqRepository
}
