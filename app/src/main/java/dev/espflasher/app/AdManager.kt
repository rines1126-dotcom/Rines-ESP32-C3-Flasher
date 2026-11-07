package dev.espflasher.app

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.AdRequest
import com.google.android.libraries.ads.mobile.sdk.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.FullScreenContentError

class AdManager {

    companion object {
        private const val TAG = "RinesAdManager"

        // Google official test interstitial ID
        private const val TEST_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/1033173712"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadInterstitialAd() {
        if (isLoading || interstitialAd != null) return

        isLoading = true

        InterstitialAd.load(
            AdRequest.Builder(TEST_AD_UNIT_ID).build(),
            object : AdLoadCallback<InterstitialAd> {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Test interstitial ad loaded.")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.e(TAG, "Ad failed to load: ${adError.message}")
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
            loadInterstitialAd()
            return
        }

        interstitialAd = null

        ad.adEventCallback =
            object : InterstitialAdEventCallback {

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.")
                    onFinished()
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) {
                    Log.e(
                        TAG,
                        "Ad failed to show: ${fullScreenContentError.message}"
                    )
                    onFinished()
                    loadInterstitialAd()
                }
            }

        ad.show(activity)
    }
}
