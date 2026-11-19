package com.example.prtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.prtv.data.AppPrefs
import com.example.prtv.data.BrandConfig
import com.example.prtv.ui.AppNavigation
import com.example.prtv.ui.Routes
import com.example.prtv.ui.theme.BrandTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPrefs
    @Inject lateinit var brand: BrandConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = when {
            !prefs.consentAccepted -> Routes.CONSENT
            // Brand fixes the channel -> go straight to the TV screen. Otherwise ask once.
            brand.channelIsFixed || prefs.channelInput.isNotBlank() -> Routes.ONAIR
            else -> Routes.SETUP
        }
        setContent {
            BrandTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(startRoute = start)
                }
            }
        }
    }
}
