package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTranscriptionDatabase(
        @ApplicationContext context: Context
    ): TranscriptionDatabase {
        return TranscriptionDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTranscriptionDao(
        database: TranscriptionDatabase
    ): TranscriptionDao {
        return database.transcriptionDao()
    }

    @Provides
    @Singleton
    fun provideProviderModelDao(
        database: TranscriptionDatabase
    ): ProviderModelDao {
        return database.providerModelDao()
    }
}
