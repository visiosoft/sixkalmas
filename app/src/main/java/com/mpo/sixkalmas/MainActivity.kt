package com.mpo.sixkalmas

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.mpo.sixkalmas.model.Kalma
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var welcomeScreen: ConstraintLayout
    private lateinit var kalmasContent: NestedScrollView
    private lateinit var startButton: MaterialButton
    
    private lateinit var titleTextView: TextView
    private lateinit var arabicTextView: TextView
    private lateinit var translationTextView: TextView
    private lateinit var translationHeadingTextView: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var speakerButton: MaterialButton
    private lateinit var switchLanguageButton: MaterialButton
    private lateinit var pageIndicator: TextView
    
    private var currentKalmaIndex = 0
    private lateinit var kalmas: List<Kalma>
    
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false
    private var isSpeaking = false
    private var isEnglish = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        // Initialize views
        welcomeScreen = findViewById(R.id.welcomeScreen)
        kalmasContent = findViewById(R.id.kalmasContent)
        startButton = findViewById(R.id.startButton)
        
        titleTextView = findViewById(R.id.kalmaTitle)
        arabicTextView = findViewById(R.id.kalmaArabic)
        translationTextView = findViewById(R.id.kalmaTranslation)
        translationHeadingTextView = findViewById(R.id.translationHeading)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        speakerButton = findViewById(R.id.speakerButton)
        switchLanguageButton = findViewById(R.id.switchLanguageButton)
        pageIndicator = findViewById(R.id.pageIndicator)

        // Create list of Kalmas
        kalmas = listOf(
            Kalma(
                getString(R.string.kalma_1_title),
                getString(R.string.kalma_1_arabic),
                getString(R.string.kalma_1_translation),
                getString(R.string.kalma_1_urdu)
            ),
            Kalma(
                getString(R.string.kalma_2_title),
                getString(R.string.kalma_2_arabic),
                getString(R.string.kalma_2_translation),
                getString(R.string.kalma_2_urdu)
            ),
            Kalma(
                getString(R.string.kalma_3_title),
                getString(R.string.kalma_3_arabic),
                getString(R.string.kalma_3_translation),
                getString(R.string.kalma_3_urdu)
            ),
            Kalma(
                getString(R.string.kalma_4_title),
                getString(R.string.kalma_4_arabic),
                getString(R.string.kalma_4_translation),
                getString(R.string.kalma_4_urdu)
            ),
            Kalma(
                getString(R.string.kalma_5_title),
                getString(R.string.kalma_5_arabic),
                getString(R.string.kalma_5_translation),
                getString(R.string.kalma_5_urdu)
            ),
            Kalma(
                getString(R.string.kalma_6_title),
                getString(R.string.kalma_6_arabic),
                getString(R.string.kalma_6_translation),
                getString(R.string.kalma_6_urdu)
            )
        )

        // Set up click listeners
        startButton.setOnClickListener {
            welcomeScreen.visibility = View.GONE
            kalmasContent.visibility = View.VISIBLE
            currentKalmaIndex = 0
            updateKalmaDisplay()
        }

        previousButton.setOnClickListener {
            if (currentKalmaIndex > 0) {
                stopSpeaking()
                currentKalmaIndex--
                updateKalmaDisplay()
            }
        }

        nextButton.setOnClickListener {
            if (currentKalmaIndex < kalmas.size - 1) {
                stopSpeaking()
                currentKalmaIndex++
                updateKalmaDisplay()
            }
        }

        speakerButton.setOnClickListener {
            if (isTtsReady) {
                if (isSpeaking) {
                    stopSpeaking()
                } else {
                    startSpeaking()
                }
            } else {
                Toast.makeText(this, "Text-to-Speech is not ready yet", Toast.LENGTH_SHORT).show()
            }
        }

        switchLanguageButton.setOnClickListener {
            isEnglish = !isEnglish
            updateKalmaDisplay()
            stopSpeaking()
            
            // Update TTS language
            if (isTtsReady) {
                val locale = if (isEnglish) Locale.US else Locale("ur")
                val result = textToSpeech.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Selected language is not supported for speech", Toast.LENGTH_SHORT).show()
                    speakerButton.isEnabled = false
                } else {
                    speakerButton.isEnabled = true
                }
            }
        }
    }

    private fun startSpeaking() {
        val textToRead = if (isEnglish) {
            kalmas[currentKalmaIndex].translation
        } else {
            kalmas[currentKalmaIndex].urduTranslation
        }
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Translation")
        textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, params, "Translation")
        isSpeaking = true
        updateSpeakingState()
    }

    private fun stopSpeaking() {
        if (isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
            updateSpeakingState()
        }
    }

    private fun updateSpeakingState() {
        speakerButton.isSelected = isSpeaking
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            speakerButton.isEnabled = isTtsReady

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Not needed for now
                }

                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        isSpeaking = false
                        updateSpeakingState()
                    }
                }

                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        isSpeaking = false
                        updateSpeakingState()
                    }
                }
            })
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
            speakerButton.isEnabled = false
        }
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    private fun updateKalmaDisplay() {
        val currentKalma = kalmas[currentKalmaIndex]
        
        // Update text views
        titleTextView.text = currentKalma.title
        arabicTextView.text = currentKalma.arabicText
        translationTextView.text = if (isEnglish) {
            currentKalma.translation
        } else {
            currentKalma.urduTranslation
        }
        
        // Update translation heading
        translationHeadingTextView.text = getString(
            if (isEnglish) R.string.translation_heading
            else R.string.translation_heading_urdu
        )
        
        // Update page indicator
        pageIndicator.text = "${currentKalmaIndex + 1}/${kalmas.size}"
        
        // Update button states
        previousButton.isEnabled = currentKalmaIndex > 0
        nextButton.isEnabled = currentKalmaIndex < kalmas.size - 1
    }
}