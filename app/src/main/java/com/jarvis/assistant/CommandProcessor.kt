package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import java.util.concurrent.Executors

class CommandProcessor(
    private val context: Context,
    private val onResponse: (String) -> Unit
) {
    private val appLauncher = AppLauncher(context)
    private val webInfoFetcher = WebInfoFetcher()
    private val ioExecutor = Executors.newSingleThreadExecutor()

    fun process(rawText: String) {
        val text = rawText.trim().lowercase()
        if (text.isEmpty()) {
            onResponse("I didn't catch that.")
            return
        }

        when {
            Regex("(open youtube and search|search .* on youtube|play .* on youtube)").containsMatchIn(text) ->
                handleYoutubeSearch(text)

            text.startsWith("open camera") -> {
                context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                onResponse("Opening camera.")
            }

            text.startsWith("open settings") -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                onResponse("Opening settings.")
            }

            text.startsWith("open ") -> handleOpenApp(text.removePrefix("open ").trim())

            text.startsWith("search for ") -> handleWebSearch(text.removePrefix("search for ").trim())
            text.startsWith("search ") -> handleWebSearch(text.removePrefix("search ").trim())
            text.startsWith("google ") -> handleWebSearch(text.removePrefix("google ").trim())

            text.startsWith("what is ") -> handleLookup(text.removePrefix("what is ").trim())
            text.startsWith("who is ") -> handleLookup(text.removePrefix("who is ").trim())
            text.startsWith("tell me about ") -> handleLookup(text.removePrefix("tell me about ").trim())

            text.startsWith("call ") -> handleCall(text.removePrefix("call ").trim())

            else -> handleLookup(text)
        }
    }

    private fun handleYoutubeSearch(text: String) {
        val query = Regex("search (?:for )?(.*?)(?: on youtube)?$")
            .find(text)?.groupValues?.get(1)?.trim()
            ?: text

        val youtubeUri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        val fallback = Intent(Intent.ACTION_VIEW, youtubeUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.packageManager.getPackageInfo("com.google.android.youtube", 0)
            context.startActivity(fallback)
        } catch (e: Exception) {
            context.startActivity(fallback)
        }
        onResponse("Searching YouTube for $query.")
    }

    private fun handleOpenApp(appName: String) {
        val opened = appLauncher.launchByName(appName)
        onResponse(if (opened) "Opening $appName." else "I couldn't find an app called $appName.")
    }

    private fun handleWebSearch(query: String) {
        if (query.isEmpty()) {
            onResponse("What would you like me to search for?")
            return
        }
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        onResponse("Searching the web for $query.")
    }

    private fun handleLookup(topic: String) {
        if (topic.isEmpty()) {
            onResponse("What would you like to know about?")
            return
        }
        onResponse("Let me look that up...")
        ioExecutor.execute {
            val summary = webInfoFetcher.fetchSummary(topic)
            onResponse(summary)
        }
    }

    private fun handleCall(numberOrName: String) {
        val uri = Uri.parse("tel:${Uri.encode(numberOrName)}")
        context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        onResponse("Opening the dialer for $numberOrName.")
    }
}
