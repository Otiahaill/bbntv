package com.example.prtv.ui.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.prtv.R
import com.example.prtv.data.AppPrefs
import com.example.prtv.ui.component.openLink
import com.example.prtv.ui.legal.LegalText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(private val prefs: AppPrefs) : ViewModel() {
    fun accept() {
        prefs.consentAccepted = true
    }
}

/** First-run gate: the user must accept the Terms and Privacy Policy before any feature is available. */
@Composable
fun ConsentScreen(
    onAccepted: () -> Unit,
    onOpenLegal: () -> Unit,
    vm: ConsentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = context.getString(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Before you start",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "This app plays YouTube videos as a scheduled, TV-style programme (not a live broadcast). " +
                    "It uses YouTube API Services. To continue you must agree to the app's Terms of Use and " +
                    "Privacy Policy, and to be bound by the YouTube Terms of Service.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onOpenLegal, modifier = Modifier.fillMaxWidth()) {
                Text("Read Terms of Use and Privacy Policy")
            }
            TextButton(
                onClick = { openLink(context, LegalText.YOUTUBE_TERMS_URL) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("YouTube Terms of Service") }
            TextButton(
                onClick = { openLink(context, LegalText.GOOGLE_PRIVACY_URL) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Google Privacy Policy") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.accept()
                    onAccepted()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("I agree and continue") }
        }
    }
}
