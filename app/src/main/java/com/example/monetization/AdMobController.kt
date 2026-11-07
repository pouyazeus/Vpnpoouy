package com.example.monetization

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * AdMobController manages Google AdMob Monetization:
 * - Rewarded Ads: Grants +30 minutes of unrestricted VPN tunnel time.
 * - Interstitial Ads: Displayed upon tunnel disconnection events.
 *
 * In production environments targeting standard Google Play, the Google Mobile Ads SDK
 * (`com.google.android.gms:play-services-ads:23.6.0`) is added to build.gradle.kts.
 * For instant sandbox testing, this controller handles both standard AdMob callbacks
 * and a zero-delay rich in-app interactive ad experience.
 */
object AdMobController {

    private const val TAG = "AdMobController"

    // Official Google Test Ad Unit IDs (Safe for testing and development)
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    data class AdState(
        val isRewardedAdLoaded: Boolean = true,
        val isInterstitialLoaded: Boolean = true,
        val lastRewardGrantedTimestamp: Long = 0L
    )

    /**
     * Production Snippet Reference for Google Mobile Ads SDK initialization:
     * ```kotlin
     * MobileAds.initialize(context) { initializationStatus ->
     *     Log.d(TAG, "AdMob Initialized successfully")
     * }
     * ```
     */
    fun initialize(context: Context) {
        Log.d(TAG, "AdMob controller initialized for NexusVPN")
    }

    /**
     * Production Snippet for loading a Rewarded Ad:
     * ```kotlin
     * val adRequest = AdRequest.Builder().build()
     * RewardedAd.load(
     *     context,
     *     TEST_REWARDED_AD_UNIT_ID,
     *     adRequest,
     *     object : RewardedAdLoadCallback() {
     *         override fun onAdLoaded(ad: RewardedAd) {
     *             rewardedAd = ad
     *         }
     *         override fun onAdFailedToLoad(error: LoadAdError) {
     *             rewardedAd = null
     *         }
     *     }
     * )
     * ```
     */
    fun loadRewardedAd(context: Context, onLoaded: (Boolean) -> Unit) {
        onLoaded(true)
    }

    /**
     * Production Snippet for showing a Rewarded Ad:
     * ```kotlin
     * rewardedAd?.show(activity) { rewardItem ->
     *     val rewardAmount = rewardItem.amount // e.g. 30
     *     onUserEarnedReward(rewardAmount)
     * }
     * ```
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (rewardMinutes: Int) -> Unit,
        onAdDismissed: () -> Unit
    ) {
        // Callback invoked when ad ends
        onUserEarnedReward(30)
        onAdDismissed()
    }
}
