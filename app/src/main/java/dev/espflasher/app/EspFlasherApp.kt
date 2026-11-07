
package dev.espflasher.app

import android.app.Application
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EspFlasherApp : Application() {

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            MobileAds.initialize(
                this@EspFlasherApp,
                InitializationConfig.Builder(
                    "ca-app-pub-8175752036808539~2580549304"
                ).build()
            ) {
                // AdMob initialization completed.
            }
        }
    }
}
