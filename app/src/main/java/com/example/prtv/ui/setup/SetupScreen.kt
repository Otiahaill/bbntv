package com.example.prtv.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.prtv.data.ApiException
import com.example.prtv.data.ApiKeyStore
import com.example.prtv.data.AppPrefs
import com.example.prtv.data.BrandConfig
import com.example.prtv.data.LineupRepository
import com.example.prtv.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SetupUi(
    val channel: String = "",
    val playlist: String = "",
    val apiKey: String = "",
    val needsKey: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: LineupRepository,
    private val prefs: AppPrefs,
    private val keyStore: ApiKeyStore,
    private val brand: BrandConfig
) : ViewModel() {

    private val _ui = MutableStateFlow(
        SetupUi(
            channel = prefs.channelInput,
            playlist = prefs.playlistInput,
            needsKey = !brand.usesProxy && !keyStore.hasBuildKey() && !keyStore.hasUserKey()
        )
    )
    val ui: StateFlow<SetupUi> = _ui.asStateFlow()

    fun onChannel(v: String) = _ui.update { it.copy(channel = v, error = null) }
    fun onPlaylist(v: String) = _ui.update { it.copy(playlist = v, error = null) }
    fun onApiKey(v: String) = _ui.update { it.copy(apiKey = v, error = null) }

    fun load() {
        val s = _ui.value
        if (s.loading) return
        if (s.channel.isBlank()) {
            _ui.update { it.copy(error = "Enter a channel @handle, URL or ID.") }
            return
        }
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (s.apiKey.isNotBlank() && !brand.usesProxy) keyStore.saveUserKey(s.apiKey)
                    repo.syncClock()
                    repo.refresh(s.channel, s.playlist)
                }
                _ui.update { it.copy(loading = false, done = true, apiKey = "") }
            } catch (e: ApiException) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun SetupScreen(
    onDone: () -> Unit,
    onOpenLegal: () -> Unit,
    vm: SetupViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Pick a channel",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "The app turns a YouTube channel into a scheduled TV channel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = ui.channel,
                onValueChange = vm::onChannel,
                label = { Text("Channel @handle, URL or ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ui.playlist,
                onValueChange = vm::onPlaylist,
                label = { Text("Playlist ID or URL (optional)") },
                supportingText = { Text("Leave empty to use the channel's uploads.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            if (ui.needsKey) {
                OutlinedTextField(
                    value = ui.apiKey,
                    onValueChange = vm::onApiKey,
                    label = { Text("Your YouTube Data API key") },
                    supportingText = { Text("Stored encrypted on this device. Create one in Google Cloud Console.") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = vm::load, enabled = !ui.loading, modifier = Modifier.fillMaxWidth()) {
                if (ui.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Load channel")
                }
            }
            TextButton(onClick = onOpenLegal, modifier = Modifier.fillMaxWidth()) {
                Text("Terms of Use and Privacy Policy")
            }
        }
    }
}
