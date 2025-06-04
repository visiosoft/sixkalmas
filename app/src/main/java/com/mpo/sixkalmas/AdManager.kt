package com.mpo.sixkalmas

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager private constructor() {
    private var interstitialAd: InterstitialAd? = null
    private var onAdClosed: (() -> Unit)? = null

    companion object {
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test ad unit ID
        private var instance: AdManager? = null

        fun getInstance(): AdManager {
            if (instance == null) {
                instance = AdManager()
            }
            return instance!!
        }
    }

    fun initialize(context: Context) {
        MobileAds.initialize(context)
        loadInterstitialAd(context)
    }

    private fun loadInterstitialAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    setupFullScreenCallback()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun setupFullScreenCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onAdClosed?.invoke()
                onAdClosed = null
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onAdClosed?.invoke()
                onAdClosed = null
            }
        }
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        this.onAdClosed = onAdClosed
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
        } else {
            onAdClosed()
        }
    }
} 