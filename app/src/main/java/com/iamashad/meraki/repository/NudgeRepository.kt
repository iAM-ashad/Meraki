package com.iamashad.meraki.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iamashad.meraki.R
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.MindfulNudge
import com.iamashad.meraki.model.NudgeType
import com.iamashad.meraki.utils.MemoryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NudgeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryManager: MemoryManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val gson = Gson()
    private var cachedNudges: List<MindfulNudge> = emptyList()

    init {
        loadNudgesFromJson()
    }

    private fun loadNudgesFromJson() {
        try {
            val jsonString = context.resources.openRawResource(R.raw.mindful_nudges)
                .bufferedReader()
                .use { it.readText() }
            
            val type = object : TypeToken<List<MindfulNudge>>() {}.type
            val rawList: List<MindfulNudge> = gson.fromJson(jsonString, type)
            
            // Phase 6 Fix: Ensure non-null text/source after deserialisation
            // GSON bypasses constructors; we must manually enforce non-null values.
            cachedNudges = rawList
                .filter { it.text != null }
                .map { nudge -> 
                    nudge.copy(source = nudge.source ?: "Meraki AI")
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getInitialNudges(): List<MindfulNudge> = withContext(ioDispatcher) {
        val nudges = mutableListOf<MindfulNudge>()

        // 1. Priority: Try to get an AI Insight from user history
        memoryManager.buildSmartNudge()?.let {
            nudges.add(MindfulNudge(it, NudgeType.INSIGHT))
        }

        // 2. Fallback/Supplement: Mix from the 300+ entries in JSON
        if (cachedNudges.isNotEmpty()) {
            val additional = cachedNudges.shuffled().take(5)
            nudges.addAll(additional)
        }
        
        // Ensure we always return a unique, shuffled list
        nudges.distinctBy { it.text }.shuffled()
    }

    suspend fun getNextNudge(): MindfulNudge = withContext(ioDispatcher) {
        if (cachedNudges.isNotEmpty()) {
            cachedNudges.random()
        } else {
            // Ultimate fallback if JSON fails
            MindfulNudge("Take a deep breath and be present.", NudgeType.AFFIRMATION)
        }
    }
}
