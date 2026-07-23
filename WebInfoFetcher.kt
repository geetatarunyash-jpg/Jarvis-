package com.jarvis.assistant

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WebInfoFetcher {

    fun fetchSummary(topic: String): String {
        return try {
            val encoded = URLEncoder.encode(topic.trim(), "UTF-8")
            val url = URL("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                json.optString("extract", "I couldn't find a clear summary for that.")
            } else {
                "I couldn't find anything on the internet for \"$topic\"."
            }
        } catch (e: Exception) {
            "I couldn't reach the internet to look that up. Check your connection."
        }
    }
}
