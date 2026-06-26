package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePreferences(private val context: Context) {
    private val prefName = "secure_vault_prefs"
    private val keyAlias = "VaultSecurityKey"
    private val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    init {
        initKeystoreKey()
    }

    private fun initKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV (12 bytes for GCM) and ciphertext
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            val ivSize = 12 // GCM IV size
            if (combined.size < ivSize) return ""
            
            val iv = ByteArray(ivSize)
            val ciphertext = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            System.arraycopy(combined, ivSize, ciphertext, 0, ciphertext.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun saveGitHubToken(token: String) {
        val encrypted = encrypt(token)
        sharedPrefs.edit().putString("github_token", encrypted).apply()
    }

    fun getGitHubToken(): String {
        val encrypted = sharedPrefs.getString("github_token", "") ?: ""
        return decrypt(encrypted)
    }

    fun saveGitHubUsername(username: String) {
        sharedPrefs.edit().putString("github_username", username).apply()
    }

    fun getGitHubUsername(): String {
        return sharedPrefs.getString("github_username", "") ?: ""
    }

    fun saveGitHubName(name: String) {
        sharedPrefs.edit().putString("github_name", name).apply()
    }

    fun getGitHubName(): String {
        return sharedPrefs.getString("github_name", "") ?: ""
    }

    fun saveGitHubEmail(email: String) {
        sharedPrefs.edit().putString("github_email", email).apply()
    }

    fun getGitHubEmail(): String {
        return sharedPrefs.getString("github_email", "") ?: ""
    }

    fun saveGitHubAvatarUrl(url: String) {
        sharedPrefs.edit().putString("github_avatar_url", url).apply()
    }

    fun getGitHubAvatarUrl(): String {
        return sharedPrefs.getString("github_avatar_url", "") ?: ""
    }

    fun saveSelectedTheme(theme: String) {
        sharedPrefs.edit().putString("selected_theme", theme).apply()
    }

    fun getSelectedTheme(): String {
        return sharedPrefs.getString("selected_theme", "ARTISTIC") ?: "ARTISTIC"
    }

    fun saveAiProvider(provider: String) {
        sharedPrefs.edit().putString("ai_provider", provider).apply()
    }

    fun getAiProvider(): String {
        return sharedPrefs.getString("ai_provider", "GEMINI") ?: "GEMINI"
    }

    fun saveGeminiApiKey(key: String) {
        val encrypted = encrypt(key)
        sharedPrefs.edit().putString("gemini_api_key", encrypted).apply()
    }

    fun getGeminiApiKey(): String {
        val encrypted = sharedPrefs.getString("gemini_api_key", "") ?: ""
        return decrypt(encrypted)
    }

    fun saveOpenRouterApiKey(key: String) {
        val encrypted = encrypt(key)
        sharedPrefs.edit().putString("openrouter_api_key", encrypted).apply()
    }

    fun getOpenRouterApiKey(): String {
        val encrypted = sharedPrefs.getString("openrouter_api_key", "") ?: ""
        return decrypt(encrypted)
    }

    fun saveOpenRouterModel(model: String) {
        sharedPrefs.edit().putString("openrouter_model", model).apply()
    }

    fun getOpenRouterModel(): String {
        return sharedPrefs.getString("openrouter_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash"
    }

    fun saveOllamaEndpoint(endpoint: String) {
        sharedPrefs.edit().putString("ollama_endpoint", endpoint).apply()
    }

    fun getOllamaEndpoint(): String {
        return sharedPrefs.getString("ollama_endpoint", "http://localhost:11434") ?: "http://localhost:11434"
    }

    fun saveOllamaModel(model: String) {
        sharedPrefs.edit().putString("ollama_model", model).apply()
    }

    fun getOllamaModel(): String {
        return sharedPrefs.getString("ollama_model", "llama3") ?: "llama3"
    }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}
