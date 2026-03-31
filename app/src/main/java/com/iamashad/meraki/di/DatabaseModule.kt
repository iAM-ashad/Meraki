package com.iamashad.meraki.di

import android.app.Application
import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.data.EmotionDao
import com.iamashad.meraki.data.SessionSummaryDao
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
 *          main-safe database operations and improved testability.
 * Phase 4: Added [SessionSummaryDao] for the long-term memory system.
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

    /**
     * Phase 3: Provides a singleton instance of EmotionDao from the Room database.
     * Used by ChatViewModel to persist [EmotionLog] entries after each classification.
     */
    @Singleton
    @Provides
    fun provideEmotionDao(application: Application): EmotionDao {
        return ChatDatabase.getInstance(application).emotionDao()
    }

    /**
     * Phase 4: Provides a singleton instance of [SessionSummaryDao] from the Room database.
     * Used by [com.iamashad.meraki.utils.MemoryManager] to persist and query compressed
     * session summaries for user-profile construction.
     */
    @Singleton
    @Provides
    fun provideSessionSummaryDao(application: Application): SessionSummaryDao {
        return ChatDatabase.getInstance(application).sessionSummaryDao()
    }
}
