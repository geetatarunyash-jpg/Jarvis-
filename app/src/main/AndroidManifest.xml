package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private val micPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else binding.statusText.text = "Microphone permission is needed for Jarvis to hear you."
    }

    private val notificationPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { startWakeWordService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this) { response -> respond(response) }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(recognitionListener)

        binding.micButton.setOnClickListener { checkPermissionAndListen() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestNotificationPermissionThenStartService()
    }

    private fun requestNotificationPermissionThenStartService() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) startWakeWordService() else notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startWakeWordService()
        }
    }

    private fun startWakeWordService() {
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            val serviceIntent = Intent(this, WakeWordService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ttsReady = true
        }
    }

    private fun checkPermissionAndListen() {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) startListening() else micPermissionRequest.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        binding.statusText.text = "Listening..."
        binding.pulseRing.visibility = android.view.View.VISIBLE
        binding.pulseRing.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse_scale))

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListeningAnimation() {
        binding.pulseRing.clearAnimation()
        binding.pulseRing.visibility = android.view.View.INVISIBLE
    }

    private fun respond(text: String) {
        runOnUiThread {
            stopListeningAnimation()
            binding.transcriptText.text = text
            binding.statusText.text = "Tap the mic and speak"
            if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val heard = matches?.firstOrNull().orEmpty()
            binding.transcriptText.text = "You said: $heard"
            commandProcessor.process(heard)
        }

        override fun onError(error: Int) {
            stopListeningAnimation()
            binding.statusText.text = "Didn't catch that — tap to try again"
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
