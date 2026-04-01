package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TranscriptionDaoTest {

    private lateinit var db: TranscriptionDatabase
    private lateinit var dao: TranscriptionDao

    @BeforeEach
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            TranscriptionDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.transcriptionDao()
    }

    @AfterEach
    fun closeDb() {
        db.close()
    }

    @Test
    fun insert_returnsValidId() = runTest {
        val entity = createTestEntity()
        val id = dao.insert(entity)
        assertTrue(id > 0)
    }

    @Test
    fun getById_returnsInsertedEntity() = runTest {
        val entity = createTestEntity(text = "Hello world")
        val id = dao.insert(entity)

        val retrieved = dao.getById(id)

        assertNotNull(retrieved)
        assertEquals("Hello world", retrieved?.text)
    }

    @Test
    fun getById_returnsNullForNonExistent() = runTest {
        val retrieved = dao.getById(999L)
        assertNull(retrieved)
    }

    @Test
    fun update_modifiesEntity() = runTest {
        val entity = createTestEntity(text = "Original")
        val id = dao.insert(entity)

        val updated = entity.copy(id = id, text = "Updated")
        dao.update(updated)

        val retrieved = dao.getById(id)
        assertEquals("Updated", retrieved?.text)
    }

    @Test
    fun deleteById_removesEntity() = runTest {
        val entity = createTestEntity()
        val id = dao.insert(entity)

        val deleted = dao.deleteById(id)

        assertEquals(1, deleted)
        assertNull(dao.getById(id))
    }

    @Test
    fun deleteById_returnsZeroForNonExistent() = runTest {
        val deleted = dao.deleteById(999L)
        assertEquals(0, deleted)
    }

    @Test
    fun incrementPlayedCount_increasesByOne() = runTest {
        val entity = createTestEntity(seen = false)
        val id = dao.insert(entity)

        dao.incrementPlayedCount(id)

        val retrieved = dao.getById(id)
        assertEquals(true, retrieved?.seen)
    }

    @Test
    fun resetPlayedCount_setsToZero() = runTest {
        val entity = createTestEntity(seen = true)
        val id = dao.insert(entity)

        dao.resetPlayedCount(id)

        val retrieved = dao.getById(id)
        assertEquals(false, retrieved?.seen)
    }

    @Test
    fun updateStatus_changesStatus() = runTest {
        val entity = createTestEntity(status = "PENDING")
        val id = dao.insert(entity)

        dao.updateStatus(id, "COMPLETED")

        val retrieved = dao.getById(id)
        assertEquals("COMPLETED", retrieved?.status)
    }

    @Test
    fun recordError_setsErrorAndIncrementsRetry() = runTest {
        val entity = createTestEntity()
        val id = dao.insert(entity)

        dao.recordError(id, "Network error")

        val retrieved = dao.getById(id)
        assertEquals("Network error", retrieved?.errorMessage)
    }

    @Test
    fun getCount_returnsCorrectCount() = runTest {
        dao.insert(createTestEntity())
        dao.insert(createTestEntity())
        dao.insert(createTestEntity())

        val count = dao.getCount()

        assertEquals(3, count)
    }

    @Test
    fun getUnviewed_returnsOnlyUnviewed() = runTest {
        dao.insert(createTestEntity(text = "Unviewed 1", seen = false))
        dao.insert(createTestEntity(text = "Viewed", seen = true))
        dao.insert(createTestEntity(text = "Unviewed 2", seen = false))

        val unviewed = dao.getUnviewed(10).first()

        assertEquals(2, unviewed.size)
        assertTrue(unviewed.all { !it.seen })
    }

    @Test
    fun searchTranscriptions_filtersByQuery() = runTest {
        dao.insert(createTestEntity(text = "Hello world"))
        dao.insert(createTestEntity(text = "Goodbye"))
        dao.insert(createTestEntity(text = "Hello there"))

        val results = dao.searchTranscriptions(
            startDate = null,
            endDate = null,
            searchQuery = "Hello",
            viewFilter = "ALL"
        ).first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.text?.contains("Hello") == true })
    }

    @Test
    fun deleteOld_removesOldEntries() = runTest {
        val oldDate = LocalDateTime.now().minusDays(100).toString()
        val recentDate = LocalDateTime.now().toString()

        dao.insert(createTestEntity(createdAt = oldDate, text = "Old"))
        dao.insert(createTestEntity(createdAt = recentDate, text = "Recent"))

        val cutoff = LocalDateTime.now().minusDays(30).toString()
        val deleted = dao.deleteOld(cutoff, "ALL")

        assertEquals(1, deleted)
        assertEquals(1, dao.getCount())
    }

    private fun createTestEntity(
        text: String = "Test",
        createdAt: String = LocalDateTime.now().toString(),
        status: String = "COMPLETED",
        seen: Boolean = false
    ): TranscriptionEntity {
        return TranscriptionEntity(
            id = 0,
            text = text,
            audioFilePath = "/test.mp3",
            createdAt = createdAt,
            status = status,
            errorMessage = null,
            seen = seen
        )
    }
}
