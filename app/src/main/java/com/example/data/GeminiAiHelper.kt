package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.security.SecurePreferences
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiHelper {
    private const val TAG = "GeminiAiHelper"
    private var isInitialized = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun initialize(context: Context, customKey: String? = null) {
        if (isInitialized && customKey == null) return
        try {
            val apiKey = if (!customKey.isNullOrEmpty()) customKey else {
                val securePrefs = SecurePreferences(context)
                val storedKey = securePrefs.getGeminiApiKey()
                if (storedKey.isNotEmpty()) storedKey else BuildConfig.GEMINI_API_KEY
            }

            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                Log.d(TAG, "Initializing Firebase with key length: ${apiKey.length}")
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.markdownvault.hgqkzp")
                    .setApiKey(apiKey)
                    .setProjectId("markdown-vault-hgqkzp")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                isInitialized = true
            } else if (!customKey.isNullOrEmpty()) {
                try {
                    apps.first().delete()
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("com.aistudio.markdownvault.hgqkzp")
                        .setApiKey(apiKey)
                        .setProjectId("markdown-vault-hgqkzp")
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                    isInitialized = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error re-initializing Firebase App", e)
                }
            } else {
                isInitialized = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase manually", e)
        }
    }

    suspend fun generateContent(context: Context, prompt: String): String = withContext(Dispatchers.IO) {
        val securePrefs = SecurePreferences(context)
        val provider = securePrefs.getAiProvider()
        Log.d(TAG, "Generating content with provider: $provider")

        when (provider) {
            "GEMINI" -> {
                val customKey = securePrefs.getGeminiApiKey()
                initialize(context, customKey.ifEmpty { null })
                try {
                    val ai = Firebase.ai
                    val model = ai.generativeModel("gemini-2.5-flash")
                    val response = model.generateContent(prompt)
                    response.text ?: ""
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating content with Gemini", e)
                    throw e
                }
            }
            "OPENROUTER" -> {
                val apiKey = securePrefs.getOpenRouterApiKey()
                val modelName = securePrefs.getOpenRouterModel()
                if (apiKey.isEmpty()) {
                    throw IllegalStateException("OpenRouter API Key is not configured in Settings.")
                }

                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val root = JSONObject().apply {
                        put("model", modelName)
                        val messagesArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                        put("messages", messagesArray)
                    }

                    val request = Request.Builder()
                        .url("https://openrouter.ai/api/v1/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("HTTP-Referer", "https://github.com/KaushalKishorMishra/markdown-vault")
                        .addHeader("X-Title", "Markdown Vault")
                        .post(root.toString().toRequestBody(mediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            throw Exception("HTTP ${response.code}: $errorBody")
                        }
                        val bodyString = response.body?.string() ?: throw Exception("Empty response from OpenRouter")
                        val responseJson = JSONObject(bodyString)
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val message = choice.getJSONObject("message")
                            message.getString("content")
                        } else {
                            ""
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating content with OpenRouter", e)
                    throw e
                }
            }
            "OLLAMA" -> {
                val endpoint = securePrefs.getOllamaEndpoint().trimEnd('/')
                val modelName = securePrefs.getOllamaModel()
                if (endpoint.isEmpty()) {
                    throw IllegalStateException("Ollama Endpoint URL is not configured in Settings.")
                }

                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val root = JSONObject().apply {
                        put("model", modelName)
                        put("prompt", prompt)
                        put("stream", false)
                    }

                    val request = Request.Builder()
                        .url("$endpoint/api/generate")
                        .post(root.toString().toRequestBody(mediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            throw Exception("HTTP ${response.code}: $errorBody")
                        }
                        val bodyString = response.body?.string() ?: throw Exception("Empty response from Ollama")
                        val responseJson = JSONObject(bodyString)
                        responseJson.getString("response")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating content with Ollama", e)
                    throw e
                }
            }
            else -> throw IllegalStateException("Unknown AI Provider: $provider")
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
