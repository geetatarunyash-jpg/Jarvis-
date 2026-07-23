package com.jarvis.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiResponder(private val context: Context) {

    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
        return prefs.getString("api_key", "") ?: ""
    }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    fun ask(question: String): String {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return "I don't have an AI key set up yet. Open Jarvis and add one in Settings."
        }

        return try {
            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", question))

            val body = JSONObject()
            body.put("model", "llama-3.3-70b-versatile")
            body.put("messages", messages)
            body.put("max_tokens", 300)

            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val choices = json.getJSONArray("choices")
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                message.getString("content")
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "I couldn't reach the AI. (${connection.responseCode}: ${errorText ?: "unknown error"})"
            }
        } catch (e: Exception) {
            "I couldn't reach the AI right now. Check your internet connection."
        }
    }
}
