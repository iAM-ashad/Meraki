package com.iamashad.meraki.di

import android.app.Application
import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies,
 * such as the DAO and repository, scoped to the SingletonComponent.
 *
 * Phase 3: ChatRepository now receives an injected @IoDispatcher for
 * main-safe database operations and improved testability.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides a singleton instance of ChatRepository.
     * The @IoDispatcher is injected by DispatchersModule so that all
     * Room operations in ChatRepository run on the IO thread.
     */
    @Singleton
    @Provides
    fun provideChatRepository(
        chatDao: ChatDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ChatRepository {
        return ChatRepository(chatDao, ioDispatcher)
    }

    /**
     * Provides a singleton instance of ChatDao by retrieving it from the Room database.
     */
    @Singleton
    @Provides
    fun provideChatDao(application: Application): ChatDao {
        return ChatDatabase.getInstance(application).chatDao()
    }
}
