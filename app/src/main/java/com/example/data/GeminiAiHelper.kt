package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiAiHelper {
    private const val TAG = "GeminiAiHelper"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val apiKey = BuildConfig.GEMINI_API_KEY
                Log.d(TAG, "Initializing Firebase manually with API key length: ${apiKey.length}")
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.markdownvault.hgqkzp")
                    .setApiKey(apiKey)
                    .setProjectId("markdown-vault-hgqkzp")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase manually", e)
        }
    }

    suspend fun generateContent(context: Context, prompt: String, modelName: String = "gemini-2.5-flash"): String = withContext(Dispatchers.IO) {
        initialize(context)
        try {
            val ai = Firebase.ai
            val model = ai.generativeModel(modelName)
            val response = model.generateContent(prompt)
            response.text ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content with model $modelName", e)
            throw e
        }
    }

    suspend fun summarizeNote(context: Context, noteTitle: String, noteContent: String): String {
        val prompt = """
            You are an expert markdown note assistant. Summarize the following markdown note.
            Provide a concise summary highlighting the main points, key takeaways, and action items.
            Format the output nicely using clean markdown.

            Note Title: $noteTitle
            Note Content:
            $noteContent
        """.trimIndent()
        return generateContent(context, prompt)
    }

    suspend fun generateTags(context: Context, noteTitle: String, noteContent: String): String {
        val prompt = """
            You are an expert markdown note assistant. Analyze the following note and suggest 3-7 relevant tags (keywords) for it.
            Output ONLY a comma-separated list of tags (e.g., programming, android, jetpack-compose). No preamble, no formatting, no extra text.

            Note Title: $noteTitle
            Note Content:
            $noteContent
        """.trimIndent()
        return generateContent(context, prompt)
    }

    suspend fun extractActionItems(context: Context, noteTitle: String, noteContent: String): String {
        val prompt = """
            You are an expert markdown note assistant. Analyze the following markdown note and extract a clean list of action items / tasks.
            If no specific action items can be found, create logical next steps based on the note's content.
            Format the output as a clean checklist of checkboxes (e.g., - [ ] Task name).

            Note Title: $noteTitle
            Note Content:
            $noteContent
        """.trimIndent()
        return generateContent(context, prompt)
    }
}
