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
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val part = JSONObject().put("text", question)
            val parts = JSONArray().put(part)
            val content = JSONObject().put("parts", parts)
            val contents = JSONArray().put(content)
            val body = JSONObject().put("contents", contents)

            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val candidates = json.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.getJSONObject("content")
                val partsArray = contentObj.getJSONArray("parts")
                partsArray.getJSONObject(0).getString("text")
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "I couldn't reach the AI. (${connection.responseCode}: ${errorText ?: "unknown error"})"
            }
        } catch (e: Exception) {
            "I couldn't reach the AI right now. Check your internet connection."
        }
    }
}
