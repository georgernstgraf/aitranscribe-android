package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionDao
import com.georgernstgraf.aitranscribe.data.local.TranscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTranscriptionRepository(
        transcriptionDao: TranscriptionDao,
        queuedTranscriptionDao: QueuedTranscriptionDao
    ): TranscriptionRepository {
        return TranscriptionRepositoryImpl(transcriptionDao, queuedTranscriptionDao)
    }
}