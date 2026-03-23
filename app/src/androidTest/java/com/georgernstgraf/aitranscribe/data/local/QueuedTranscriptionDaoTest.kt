package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class QueuedTranscriptionDaoTest {

    private lateinit var db: TranscriptionDatabase
    private lateinit var dao: QueuedTranscriptionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            TranscriptionDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.queuedTranscriptionDao()
    }

    @After
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
        val entity = createTestEntity(audioPath = "/test.mp3")
        val id = dao.insert(entity)

        val retrieved = dao.getById(id)

        assertNotNull(retrieved)
        assertEquals("/test.mp3", retrieved?.audioFilePath)
    }

    @Test
    fun getById_returnsNullForNonExistent() = runTest {
        val retrieved = dao.getById(999L)
        assertNull(retrieved)
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
    fun getCount_returnsCorrectCount() = runTest {
        dao.insert(createTestEntity(audioPath = "/audio1.mp3"))
        dao.insert(createTestEntity(audioPath = "/audio2.mp3"))

        assertEquals(2, dao.getCount())
    }

    @Test
    fun clearAll_removesAllEntities() = runTest {
        dao.insert(createTestEntity())
        dao.insert(createTestEntity())

        val cleared = dao.clearAll()

        assertEquals(2, cleared)
        assertEquals(0, dao.getCount())
    }

    @Test
    fun getAll_returnsAllInOrder() = runTest {
        dao.insert(createTestEntity(audioPath = "/audio1.mp3"))
        dao.insert(createTestEntity(audioPath = "/audio2.mp3"))

        val all = dao.getAll().first()

        assertEquals(2, all.size)
    }

    private fun createTestEntity(
        audioPath: String = "/test.mp3",
        createdAt: String = LocalDateTime.now().toString()
    ): QueuedTranscriptionEntity {
        return QueuedTranscriptionEntity(
            id = 0,
            audioFilePath = audioPath,
            sttModel = "whisper-large-v3",
            llmModel = null,
            postProcessingType = null,
            createdAt = createdAt,
            priority = 0
        )
    }
}
