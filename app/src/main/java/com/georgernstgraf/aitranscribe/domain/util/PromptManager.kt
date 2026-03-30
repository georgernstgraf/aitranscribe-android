package com.georgernstgraf.aitranscribe.domain.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prompts: Map<String, String>

    init {
        prompts = try {
            val jsonString = context.assets.open("prompts.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Error: prompts.json missing or corrupt!", Toast.LENGTH_LONG).show()
            }
            throw IllegalStateException("Failed to load prompts.json", e)
        }
    }

    fun get(key: String): String {
        return prompts[key] ?: throw IllegalArgumentException("Missing prompt key: $key")
    }
}
