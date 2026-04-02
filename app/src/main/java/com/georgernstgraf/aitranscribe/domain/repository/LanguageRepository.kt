package com.georgernstgraf.aitranscribe.domain.repository

import com.georgernstgraf.aitranscribe.data.local.LanguageDao
import com.georgernstgraf.aitranscribe.data.local.LanguageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Language(
    val id: String,
    val name: String,
    val nativeName: String?,
    val isActive: Boolean
)

fun LanguageEntity.toDomain(): Language {
    return Language(
        id = id,
        name = name,
        nativeName = nativeName,
        isActive = isActive
    )
}

interface LanguageRepository {
    fun getActiveLanguages(): Flow<List<Language>>
    fun getAllLanguages(): Flow<List<Language>>
    suspend fun getLanguageById(id: String): Language?
    suspend fun getLanguageName(id: String): String
    suspend fun ensureLanguageExists(id: String): Language
    suspend fun setLanguageActive(id: String, isActive: Boolean)
    suspend fun getActiveLanguageCount(): Int
}

@Singleton
class LanguageRepositoryImpl @Inject constructor(
    private val languageDao: LanguageDao
) : LanguageRepository {

    override fun getActiveLanguages(): Flow<List<Language>> {
        return languageDao.getActiveLanguages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllLanguages(): Flow<List<Language>> {
        return languageDao.getAllLanguages().map { entities ->
            entities.map { it.toDomain() }
                .sortedWith(
                    compareByDescending<Language> { it.isActive }
                        .thenBy { it.name }
                )
        }
    }

    override suspend fun getLanguageById(id: String): Language? {
        return languageDao.getLanguageById(id)?.toDomain()
    }

    override suspend fun getLanguageName(id: String): String {
        return languageDao.getLanguageName(id) ?: id.uppercase()
    }

    override suspend fun ensureLanguageExists(id: String): Language {
        // Check if language already exists
        val existing = languageDao.getLanguageById(id)
        if (existing != null) {
            return existing.toDomain()
        }

        // Create new language entry with ISO code as display name (uppercased)
        val newLanguage = LanguageEntity(
            id = id,
            name = id.uppercase(),
            nativeName = null,
            isActive = true
        )
        languageDao.insertLanguage(newLanguage)
        return newLanguage.toDomain()
    }

    override suspend fun setLanguageActive(id: String, isActive: Boolean) {
        languageDao.updateLanguageActiveStatus(id, isActive)
    }

    override suspend fun getActiveLanguageCount(): Int {
        return languageDao.getActiveLanguageCount()
    }
}
