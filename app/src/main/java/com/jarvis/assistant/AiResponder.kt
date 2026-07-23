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
            val url = URL("https://api.anthropic.com/v1/messages")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", apiKey)
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", question))

            val body = JSONObject()
            body.put("model", "claude-sonnet-4-6")
            body.put("max_tokens", 300)
            body.put("messages", messages)

            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val content = json.getJSONArray("content")
                content.getJSONObject(0).getString("text")
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "I couldn't reach the AI. (${connection.responseCode}: ${errorText ?: "unknown error"})"
            }
        } catch (e: Exception) {
            "I couldn't reach the AI right now. Check your internet connection."
        }
    }
}
