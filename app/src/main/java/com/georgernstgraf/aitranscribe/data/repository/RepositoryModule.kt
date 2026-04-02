package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.LanguageDao
import com.georgernstgraf.aitranscribe.data.local.TranscriptionDao
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepositoryImpl
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
        transcriptionDao: TranscriptionDao
    ): TranscriptionRepository {
        return TranscriptionRepositoryImpl(transcriptionDao)
    }

    @Provides
    @Singleton
    fun provideLanguageRepository(
        languageDao: LanguageDao
    ): LanguageRepository {
        return LanguageRepositoryImpl(languageDao)
    }
}
