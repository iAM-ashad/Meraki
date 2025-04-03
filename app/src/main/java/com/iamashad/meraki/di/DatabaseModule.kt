package com.iamashad.meraki.di

import android.app.Application
import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies,
 * such as the DAO and repository, scoped to the SingletonComponent.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides a singleton instance of ChatRepository using the injected ChatDao.
     *
     * @param chatDao The DAO used by the repository for data operations.
     * @return ChatRepository instance.
     */
    @Singleton
    @Provides
    fun provideChatRepository(chatDao: ChatDao): ChatRepository {
        return ChatRepository(chatDao)
    }

    /**
     * Provides a singleton instance of ChatDao by retrieving it from the Room database.
     *
     * @param application The Application context needed to build the database.
     * @return ChatDao instance.
     */
    @Singleton
    @Provides
    fun provideChatDao(application: Application): ChatDao {
        return ChatDatabase.getInstance(application).chatDao()
    }
}
