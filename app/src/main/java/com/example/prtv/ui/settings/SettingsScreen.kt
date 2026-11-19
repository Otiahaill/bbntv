package com.example.prtv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.prtv.data.ApiKeyStore
import com.example.prtv.data.AppPrefs
import com.example.prtv.data.BrandConfig
import com.example.prtv.data.LineupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUi(
    val hasUserKey: Boolean = false,
    val hasBuildKey: Boolean = false,
    val showKeySection: Boolean = true,
    val showChangeChannel: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStore: ApiKeyStore,
    private val prefs: AppPrefs,
    private val repo: LineupRepository,
    brand: BrandConfig
) : ViewModel() {

    private val _ui = MutableStateFlow(
        SettingsUi(
            hasUserKey = keyStore.hasUserKey(),
            hasBuildKey = keyStore.hasBuildKey(),
            showKeySection = !brand.usesProxy,
            showChangeChannel = !brand.channelIsFixed
        )
    )
    val ui: StateFlow<SettingsUi> = _ui.asStateFlow()

    fun saveKey(key: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { keyStore.saveUserKey(key) }
            _ui.value = _ui.value.copy(hasUserKey = keyStore.hasUserKey())
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { keyStore.clearUserKey() }
            _ui.value = _ui.value.copy(hasUserKey = false)
        }
    }

    /** Deletes everything this app stores: cached video details, channel choice, key and consent. */
    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.clear()
                keyStore.clearUserKey()
                prefs.clearAll()
            }
            onDone()
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeChannel: () -> Unit,
    onOpenLegal: () -> Unit,
    onDataCleared: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var newKey by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (ui.showChangeChannel) {
                OutlinedButton(onClick = onChangeChannel, modifier = Modifier.fillMaxWidth()) {
                    Text("Change channel or playlist")
                }
            }
            OutlinedButton(onClick = onOpenLegal, modifier = Modifier.fillMaxWidth()) {
                Text("Terms, privacy and legal")
            }

            if (ui.showKeySection) {
                HorizontalDivider()

                Text(
                    "YouTube API key",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    when {
                        ui.hasUserKey -> "Using the key you entered (stored encrypted on this device)."
                        ui.hasBuildKey -> "Using the key built into this app."
                        else -> "No key set. Enter one to load channels."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    label = { Text("Your own API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        vm.saveKey(newKey)
                        newKey = ""
                    },
                    enabled = newKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save key") }
                if (ui.hasUserKey) {
                    OutlinedButton(onClick = vm::removeKey, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove my key")
                    }
                }

                HorizontalDivider()
            }

            Text(
                "Your data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Clear stored data deletes everything this app keeps on this device: cached video details, " +
                    "your channel choice, your API key and your consent. It does not change anything stored by " +
                    "YouTube or Google; manage that in YouTube and your Google account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { vm.clearAll(onDataCleared) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear stored data") }
        }
    }
}
