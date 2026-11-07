
package dev.espflasher.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager {

    companion object {
        private const val TAG = "RinesAdManager"

        // Google's official test interstitial ad unit ID
        private const val TEST_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/1033173712"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadInterstitialAd(context: Context) {
        if (isLoading || interstitialAd != null) return

        isLoading = true

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            TEST_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Test interstitial ad loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.e(TAG, "Ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialAd(
        activity: Activity,
        onFinished: () -> Unit
    ) {
        val ad = interstitialAd

        if (ad == null) {
            Log.d(TAG, "Ad not ready. Continue without ad.")
            onFinished()
            loadInterstitialAd(activity)
            return
        }

        interstitialAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.")
                    onFinished()
                    loadInterstitialAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    onFinished()
                    loadInterstitialAd(activity)
                }
            }

        ad.show(activity)
    }
}
