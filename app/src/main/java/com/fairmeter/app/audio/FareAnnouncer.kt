package com.fairmeter.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class FareAnnouncer(context: Context) {

    private val tts: TextToSpeech
    private var isInitialized = false
    private var isEnabled = true
    private var selectedLocale: Locale = Locale.ENGLISH

    private val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale("hi"),  // Hindi
        Locale("kn"),  // Kannada
        Locale("ta"),  // Tamil
        Locale("te"),  // Telugu
        Locale("mr")   // Marathi
    )

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setLanguage(locale: Locale) {
        selectedLocale = locale
        if (isInitialized) {
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                fallbackLanguage()
            }
        }
    }

    private fun fallbackLanguage() {
        for (locale in supportedLocales) {
            val result = tts.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                selectedLocale = locale
                return
            }
        }
    }

    fun speakFare(amount: Int) {
        if (!isEnabled || !isInitialized) return
        val text = buildFareUtterance(amount)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fare_utterance")
    }

    private fun buildFareUtterance(amount: Int): String {
        return when (selectedLocale) {
            Locale("hi") -> "किराया $amount रुपये है"
            Locale("kn") -> "ಬಾಡಿಗೆ $amount ರೂಪಾಯಿ"
            Locale("ta") -> "கட்டணம் $amount ரூபாய்"
            Locale("te") -> "ఛార్జీ $amount రూపాయలు"
            Locale("mr") -> "भाडे $amount रुपये आहे"
            else -> "Fare is $amount rupees"
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
