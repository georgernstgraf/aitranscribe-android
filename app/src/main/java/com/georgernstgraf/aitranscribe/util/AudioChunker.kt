package com.georgernstgraf.aitranscribe.util

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for chunking large audio files into smaller segments.
 * Uses native Android APIs (MediaExtractor, MediaMuxer) instead of FFmpeg.
 *
 * @param context Application context for file operations
 * @param maxFileSizeMB Maximum file size in MB for each chunk (default: 25)
 */
@Singleton
class AudioChunker @Inject constructor(
    private val context: Context,
    private val maxFileSizeMB: Int = 25
) {

    companion object {
        private const val CHUNK_DURATION_SECONDS = 600 // 10 minutes per chunk
    }

    /**
     * Splits an audio file into chunks smaller than maxFileSizeMB.
     *
     * @param filePath Path to the audio file
     * @return List of chunk file paths (or single path if file is small enough)
     * @throws IllegalArgumentException if file doesn't exist
     * @throws Exception if chunking fails
     */
    suspend fun chunkAudio(filePath: String): List<String> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        
        if (!file.exists()) {
            throw IllegalArgumentException("Audio file does not exist: $filePath")
        }

        val fileSizeMB = file.length() / (1024.0 * 1024.0)
        
        // If file is small enough, return original path
        if (fileSizeMB <= maxFileSizeMB) {
            return@withContext listOf(filePath)
        }

        // Get audio duration
        val durationUs = getAudioDuration(filePath)
        val durationSeconds = durationUs / 1_000_000.0
        
        // Calculate number of chunks needed
        val chunkCount = calculateChunkCount(durationSeconds, fileSizeMB)
        
        // Create chunks
        val chunks = mutableListOf<String>()
        val chunkDurationUs = (durationUs / chunkCount).toLong()
        
        for (i in 0 until chunkCount) {
            val chunkPath = createChunkPath(file, i)
            val startTimeUs = i * chunkDurationUs
            val endTimeUs = if (i == chunkCount - 1) {
                durationUs // Last chunk goes to end
            } else {
                (i + 1) * chunkDurationUs
            }
            
            createChunk(
                inputPath = filePath,
                outputPath = chunkPath,
                startTimeUs = startTimeUs,
                endTimeUs = endTimeUs
            )
            
            chunks.add(chunkPath)
        }

        chunks
    }

    /**
     * Gets the duration of an audio file in microseconds.
     *
     * @param filePath Path to the audio file
     * @return Duration in microseconds
     * @throws Exception if unable to get duration
     */
    suspend fun getAudioDuration(filePath: String): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            throw Exception("Unable to get audio duration: ${e.message}", e)
        } finally {
            retriever.release()
        }
    }

    /**
     * Calculates the number of chunks needed based on duration and file size.
     */
    private fun calculateChunkCount(duration: Double, fileSizeMB: Double): Int {
        // Start with 10-minute segments
        var chunkCount = (duration / CHUNK_DURATION_SECONDS).toInt().coerceAtLeast(1)
        
        // If chunks are still too large, increase count
        val estimatedChunkSize = fileSizeMB / chunkCount
        if (estimatedChunkSize > maxFileSizeMB) {
            chunkCount = (fileSizeMB / maxFileSizeMB).toInt() + 1
        }
        
        return chunkCount
    }

    /**
     * Creates a chunk file path.
     */
    private fun createChunkPath(originalFile: File, index: Int): String {
        val parentDir = originalFile.parent
        val baseName = originalFile.nameWithoutExtension
        val extension = originalFile.extension
        val uniqueId = UUID.randomUUID().toString().take(8)
        
        return "$parentDir/${baseName}_chunk${index}_$uniqueId.$extension"
    }

    /**
     * Creates a chunk using MediaExtractor and MediaMuxer.
     * Note: This is a simplified implementation that copies the audio stream.
     * For production use, consider handling multiple tracks, metadata, etc.
     */
    private suspend fun createChunk(
        inputPath: String,
        outputPath: String,
        startTimeUs: Long,
        endTimeUs: Long
    ) = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        val outputFile = File(outputPath)
        
        val extractor = MediaExtractor()
        val muxer = MediaMuxer(outputPath.absolutePath)
        
        try {
            extractor.setDataSource(inputPath)
            
            // Find audio track
            val audioTrackIndex = findAudioTrackIndex(extractor)
            if (audioTrackIndex == -1) {
                throw Exception("No audio track found in file")
            }
            
            // Get track format
            val trackFormat = extractor.getTrackFormat(audioTrackIndex)
            
            // Add track to muxer
            val muxerTrackIndex = muxer.addTrack(trackFormat)
            muxer.start()
            
            // Seek to start time
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            
            // Select the track
            extractor.selectTrack(audioTrackIndex)
            
            // Copy data
            val bufferInfo = MediaCodec.BufferInfo()
            val maxBufferSize = 1024 * 1024 // 1MB buffer
            val buffer = java.nio.ByteBuffer.allocate(maxBufferSize)
            
            var sampleTimeUs: Long = -1
            while (sampleTimeUs < endTimeUs) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                
                if (sampleSize < 0) {
                    // End of stream
                    break
                }
                
                extractor.getSampleTime(bufferInfo)
                sampleTimeUs = bufferInfo.presentationTimeUs
                
                if (sampleTimeUs >= endTimeUs) {
                    // Reached end of chunk
                    break
                }
                
                // Skip samples before start time (after seeking)
                if (sampleTimeUs >= startTimeUs) {
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs -= startTimeUs
                    
                    muxer.writeSampleData(buffer, bufferInfo, muxerTrackIndex)
                }
            }
            
        } catch (e: Exception) {
            // Clean up on error
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw Exception("Failed to create chunk: ${e.message}", e)
        } finally {
            muxer.stop()
            muxer.release()
            extractor.release()
        }
    }
    
    /**
     * Finds the audio track index in the media file.
     */
    private fun findAudioTrackIndex(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }
}
