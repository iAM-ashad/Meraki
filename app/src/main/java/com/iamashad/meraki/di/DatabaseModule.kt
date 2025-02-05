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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideChatRepository(chatDao: ChatDao): ChatRepository {
        return ChatRepository(chatDao)
    }

    @Singleton
    @Provides
    fun provideChatDao(application: Application): ChatDao {
        return ChatDatabase.getInstance(application).chatDao()
    }
}