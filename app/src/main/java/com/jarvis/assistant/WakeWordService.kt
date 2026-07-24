package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class WakeWordService : Service(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private val restartDelayMs = 1200L

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        tts = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this) { response ->
            if (ttsReady) tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(recognitionListener)
        startListeningLoop()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ttsReady = true
        }
    }

    private fun startForegroundNotification() {
        val channelId = "jarvis_wake_word"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "Jarvis listening", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis is listening")
            .setContentText("Say \"Jarvis\" to activate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startListeningLoop() {
        if (isListening) return
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        handler.postDelayed({ startListeningLoop() }, restartDelayMs)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val heard = matches?.firstOrNull()?.lowercase().orEmpty()

            if (heard.contains("jarvis")) {
                val command = heard.replace("jarvis", "", ignoreCase = true).trim()
                if (command.isNotEmpty()) {
                    commandProcessor.process(command)
                } else {
                    if (ttsReady) tts.speak("Yes sir?", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
            scheduleRestart()
        }

        override fun onError(error: Int) {
            isListening = false
            scheduleRestart()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
