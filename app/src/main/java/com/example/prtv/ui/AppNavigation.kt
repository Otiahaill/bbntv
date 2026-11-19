package com.example.prtv.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.prtv.ui.consent.ConsentScreen
import com.example.prtv.ui.legal.LegalScreen
import com.example.prtv.ui.onair.OnAirScreen
import com.example.prtv.ui.settings.SettingsScreen
import com.example.prtv.ui.setup.SetupScreen

object Routes {
    const val CONSENT = "consent"
    const val SETUP = "setup"
    const val ONAIR = "onair"
    const val SETTINGS = "settings"
    const val LEGAL = "legal"
}

/** Clears the back stack and goes to [route]. */
private fun NavController.resetTo(route: String) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

@Composable
fun AppNavigation(startRoute: String) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startRoute) {
        composable(Routes.CONSENT) {
            ConsentScreen(
                onAccepted = { nav.resetTo(Routes.ONAIR) },
                onOpenLegal = { nav.navigate(Routes.LEGAL) }
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                onDone = { nav.resetTo(Routes.ONAIR) },
                onOpenLegal = { nav.navigate(Routes.LEGAL) }
            )
        }
        composable(Routes.ONAIR) {
            OnAirScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onNeedsSetup = { nav.resetTo(Routes.SETUP) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onChangeChannel = { nav.navigate(Routes.SETUP) },
                onOpenLegal = { nav.navigate(Routes.LEGAL) },
                onDataCleared = { nav.resetTo(Routes.CONSENT) }
            )
        }
        composable(Routes.LEGAL) {
            LegalScreen(onBack = { nav.popBackStack() })
        }
    }
}
