package com.mpo.sixkalmas

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.mpo.sixkalmas.model.Kalma
import java.util.*
import android.app.Dialog
import android.view.Window
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var welcomeScreen: ConstraintLayout
    private lateinit var kalmasContent: NestedScrollView
    private lateinit var topBanner: LinearLayout
    private lateinit var nativeAdContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var titleTextView: TextView
    private lateinit var arabicTextView: TextView
    private lateinit var translationTextView: TextView
    private lateinit var translationHeadingTextView: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var speakerButton: MaterialButton
    private lateinit var switchLanguageButton: MaterialButton
    private lateinit var pageIndicator: TextView
    private lateinit var themeToggleButton: ImageButton
    
    private var currentKalmaIndex = 0
    private lateinit var kalmas: List<Kalma>
    private var nativeAd: NativeAd? = null
    private var mInterstitialAd: InterstitialAd? = null
    
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false
    private var isSpeaking = false
    private var isEnglish = true
    private var isDarkMode = false
    private lateinit var sharedPreferences: SharedPreferences
    private var currentTextSize = 1.0f
    private val TEXT_SIZE_STEP = 0.1f
    private val MAX_TEXT_SIZE = 1.5f
    private val MIN_TEXT_SIZE = 0.8f

    companion object {
        private const val PREFS_NAME = "SixKalmasPrefs"
        private const val THEME_MODE = "theme_mode"
        private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" // Test ad unit ID
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test ad unit ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AdMob
        MobileAds.initialize(this) { initializationStatus ->
            // Load native ad after initialization
            loadNativeAd()
            // Load interstitial ad
            loadInterstitialAd()
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isDarkMode = sharedPreferences.getBoolean(THEME_MODE, false)
        
        // Apply theme
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_main)

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        // Initialize views
        welcomeScreen = findViewById(R.id.welcomeScreen)
        kalmasContent = findViewById(R.id.kalmasContent)
        topBanner = findViewById(R.id.topBanner)
        nativeAdContainer = findViewById(R.id.nativeAdContainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Hide bottom navigation and top banner initially
        bottomNavigation.visibility = View.GONE
        topBanner.visibility = View.GONE
        
        titleTextView = findViewById(R.id.kalmaTitle)
        arabicTextView = findViewById(R.id.kalmaArabic)
        translationTextView = findViewById(R.id.kalmaTranslation)
        translationHeadingTextView = findViewById(R.id.translationHeading)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        speakerButton = findViewById(R.id.speakerButton)
        switchLanguageButton = findViewById(R.id.switchLanguageButton)
        pageIndicator = findViewById(R.id.pageIndicator)
        themeToggleButton = findViewById(R.id.themeToggleButton)
        
        // Set initial theme toggle button state
        themeToggleButton.isActivated = isDarkMode

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
        welcomeScreen.setOnClickListener {
            welcomeScreen.visibility = View.GONE
            kalmasContent.visibility = View.VISIBLE
            topBanner.visibility = View.VISIBLE
            bottomNavigation.visibility = View.VISIBLE
            currentKalmaIndex = 0
            updateKalmaDisplay()
        }

        // Initialize bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.visibility = View.GONE // Hide initially
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_qibla -> {
                    // Show interstitial ad before opening Qibla finder
                    AdManager.getInstance().showInterstitialAd(this) {
                        startActivity(Intent(this, QiblaFinderActivity::class.java))
                    }
                    false // Don't change selection until ad is shown
                }
                R.id.navigation_kalma -> {
                    // Show ad before staying in Kalma view
                    AdManager.getInstance().showInterstitialAd(this) {
                        // No action needed, already in Kalma view
                    }
                    false // Don't change selection until ad is shown
                }
                R.id.navigation_prayer -> {
                    // Show ad before showing prayer timings message
                    AdManager.getInstance().showInterstitialAd(this) {
                        Toast.makeText(this, "Prayer Timings coming soon!", Toast.LENGTH_SHORT).show()
                    }
                    false // Don't change selection until ad is shown
                }
                else -> false
            }
        }

        // Set Kalma as selected
        bottomNavigation.selectedItemId = R.id.navigation_kalma

        // Set up click listeners
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

        themeToggleButton.setOnClickListener {
            showInterstitialAd {
                toggleTheme()
            }
        }

        // Initialize text size buttons
        findViewById<ImageButton>(R.id.decreaseTextSizeButton).setOnClickListener {
            adjustTextSize(false)
        }

        findViewById<ImageButton>(R.id.increaseTextSizeButton).setOnClickListener {
            adjustTextSize(true)
        }

        // Initialize AdManager
        AdManager.getInstance().initialize(this)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    setupInterstitialAdCallbacks()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    private fun setupInterstitialAdCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Load the next interstitial ad
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mInterstitialAd = null
            }
        }
    }

    private fun showInterstitialAd(onAdClosed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdClosed()
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdClosed()
                    loadInterstitialAd()
                }
            }
        } else {
            onAdClosed()
            loadInterstitialAd()
        }
    }

    private fun toggleTheme() {
        isDarkMode = !isDarkMode
        
        // Save the current state
        val currentPosition = currentKalmaIndex
        val wasSpeaking = isSpeaking
        val wasEnglish = isEnglish
        
        // Apply theme
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        // Save theme preference
        sharedPreferences.edit().putBoolean(THEME_MODE, isDarkMode).apply()
        
        // Update theme toggle button state
        themeToggleButton.isActivated = isDarkMode
        
        // Force update the current view
        recreate()
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

    private fun loadNativeAd() {
        val adLoader = AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad: NativeAd ->
                // Clean up the previous ad
                nativeAd?.destroy()
                nativeAd = ad
                
                // Inflate the native ad layout
                val adView = layoutInflater.inflate(
                    R.layout.native_ad_layout,
                    nativeAdContainer,
                    false
                ) as NativeAdView

                // Populate the native ad view
                populateNativeAdView(ad, adView)

                // Remove any existing views from the container
                nativeAdContainer.removeAllViews()
                
                // Add the ad view to the container
                nativeAdContainer.addView(adView)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Handle the error
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load native ad: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.mediaView?.setMediaContent(nativeAd.mediaContent)

        // Set other ad assets
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every NativeAd
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them
        nativeAd.body?.let {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = it
        } ?: run {
            adView.bodyView?.visibility = View.INVISIBLE
        }

        nativeAd.callToAction?.let {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as MaterialButton).text = it
        } ?: run {
            adView.callToActionView?.visibility = View.INVISIBLE
        }

        nativeAd.icon?.let {
            adView.iconView?.visibility = View.VISIBLE
            (adView.iconView as ImageView).setImageDrawable(it.drawable)
        } ?: run {
            adView.iconView?.visibility = View.INVISIBLE
        }

        nativeAd.price?.let {
            adView.priceView?.visibility = View.VISIBLE
            (adView.priceView as TextView).text = it
        } ?: run {
            adView.priceView?.visibility = View.INVISIBLE
        }

        nativeAd.store?.let {
            adView.storeView?.visibility = View.VISIBLE
            (adView.storeView as TextView).text = it
        } ?: run {
            adView.storeView?.visibility = View.INVISIBLE
        }

        nativeAd.starRating?.let {
            adView.starRatingView?.visibility = View.VISIBLE
            (adView.starRatingView as RatingBar).rating = it.toFloat()
        } ?: run {
            adView.starRatingView?.visibility = View.INVISIBLE
        }

        nativeAd.advertiser?.let {
            adView.advertiserView?.visibility = View.VISIBLE
            (adView.advertiserView as TextView).text = it
        } ?: run {
            adView.advertiserView?.visibility = View.INVISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad
        adView.setNativeAd(nativeAd)
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Only handle theme changes
        if (newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK != 
            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            
            // Update theme toggle button state
            themeToggleButton.isActivated = isDarkMode
            
            // Update the current view
            updateKalmaDisplay()
        }
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

    private fun adjustTextSize(increase: Boolean) {
        val kalmaArabic = findViewById<TextView>(R.id.kalmaArabic)
        val kalmaTranslation = findViewById<TextView>(R.id.kalmaTranslation)
        val translationHeading = findViewById<TextView>(R.id.translationHeading)

        if (increase) {
            if (currentTextSize < MAX_TEXT_SIZE) {
                currentTextSize += TEXT_SIZE_STEP
            }
        } else {
            if (currentTextSize > MIN_TEXT_SIZE) {
                currentTextSize -= TEXT_SIZE_STEP
            }
        }

        // Get the original text sizes
        val originalArabicSize = resources.getDimension(R.dimen.kalma_arabic_text_size)
        val originalTranslationSize = resources.getDimension(R.dimen.kalma_translation_text_size)
        val originalHeadingSize = resources.getDimension(R.dimen.translation_heading_text_size)

        // Apply the scaled sizes
        kalmaArabic.textSize = originalArabicSize * currentTextSize / resources.displayMetrics.density
        kalmaTranslation.textSize = originalTranslationSize * currentTextSize / resources.displayMetrics.density
        translationHeading.textSize = originalHeadingSize * currentTextSize / resources.displayMetrics.density
    }

    private fun showQiblaFinderBanner() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.qibla_finder_banner, null)
        dialog.setContentView(view)

        // Set up the Qibla finder button click
        view.findViewById<Button>(R.id.qiblaFinderButton).setOnClickListener {
            dialog.dismiss()
            // Show interstitial ad before opening Qibla finder
            AdManager.getInstance().showInterstitialAd(this) {
                // This will be called when the ad is closed or fails to show
                startActivity(Intent(this, QiblaFinderActivity::class.java))
            }
        }

        dialog.show()
    }
}