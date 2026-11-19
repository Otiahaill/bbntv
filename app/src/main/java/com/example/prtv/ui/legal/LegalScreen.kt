package com.example.prtv.ui.legal

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.prtv.R
import com.example.prtv.ui.component.openLink

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val contact = context.getString(R.string.contact_email)
    val terms = remember { LegalText.terms }
    val privacy = remember(contact) { LegalText.privacy(contact) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    "Terms of Use",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(terms) { LegalBlock(it) }
            item {
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(privacy) { LegalBlock(it) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                    OutlinedButton(
                        onClick = { openLink(context, LegalText.YOUTUBE_TERMS_URL) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("YouTube Terms of Service") }
                    OutlinedButton(
                        onClick = { openLink(context, LegalText.GOOGLE_PRIVACY_URL) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Google Privacy Policy") }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
                }
            }
        }
    }
}

/** Focusable so D-pad users can scroll through long text. */
@Composable
private fun LegalBlock(section: LegalSection) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            section.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            section.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
