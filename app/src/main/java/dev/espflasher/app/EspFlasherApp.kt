
package dev.espflasher.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

class EspFlasherApp : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) {
            // Google Mobile Ads SDK initialized.
        }
    }
}
