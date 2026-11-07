
package dev.espflasher.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.espflasher.app.UpdateInfo
import dev.espflasher.app.UpdateManager
import dev.espflasher.app.domain.prefs.Accent
import dev.espflasher.app.domain.prefs.ThemeMode
import dev.espflasher.app.ui.i18n.AppLanguages
import dev.espflasher.app.ui.i18n.LocalUiText
import dev.espflasher.app.ui.theme.accentSwatch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: FlasherViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val t = LocalUiText.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dynamicOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(t.settings) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(t.back)
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(t.language, style = MaterialTheme.typography.titleSmall)

            Text(
                t.languageHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguages.forEach { lang ->
                    val label =
                        if (lang.tag.isEmpty()) t.languageSystem
                        else lang.nativeName

                    FilterChip(
                        selected = state.languageTag == lang.tag,
                        onClick = { vm.setLanguage(lang.tag) },
                        label = { Text(label) },
                    )
                }
            }

            Text(t.appearance, style = MaterialTheme.typography.titleSmall)

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.themeMode == ThemeMode.SYSTEM,
                    onClick = { vm.setThemeMode(ThemeMode.SYSTEM) },
                    label = { Text(t.appearanceSystem) },
                )

                FilterChip(
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = { vm.setThemeMode(ThemeMode.LIGHT) },
                    label = { Text(t.appearanceLight) },
                )

                FilterChip(
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = { vm.setThemeMode(ThemeMode.DARK) },
                    label = { Text(t.appearanceDark) },
                )
            }

            Text(t.color, style = MaterialTheme.typography.titleSmall)

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dynamicOk) {
                    AccentDot(
                        selected = state.accent == Accent.DYNAMIC,
                        label = t.colorDynamic,
                        dynamic = true,
                        color = accentSwatch(Accent.TEAL),
                        onClick = { vm.setAccent(Accent.DYNAMIC) },
                    )
                }

                listOf(
                    Accent.TEAL to t.colorTeal,
                    Accent.RASPBERRY to t.colorRaspberry,
                    Accent.INDIGO to t.colorIndigo,
                    Accent.AMBER to t.colorAmber,
                    Accent.FOREST to t.colorForest,
                ).forEach { (accent, label) ->
                    AccentDot(
                        selected = state.accent == accent,
                        label = label,
                        dynamic = false,
                        color = accentSwatch(accent),
                        onClick = { vm.setAccent(accent) },
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(t.keepAwake)
                Switch(
                    checked = state.keepAwake,
                    onCheckedChange = { vm.setKeepAwake(it) },
                )
            }

            HorizontalDivider()

            Text(
                "App Updates",
                style = MaterialTheme.typography.titleSmall,
            )

            Button(
                onClick = {
                    scope.launch {
                        isCheckingUpdate = true
                        updateMessage = null
                        availableUpdate = null

                        try {
                            val result =
                                UpdateManager.checkForUpdate(context)

                            if (result != null) {
                                availableUpdate = result
                            } else {
                                updateMessage =
                                    "Walang available na bagong update."
                            }
                        } catch (e: Exception) {
                            updateMessage =
                                "Hindi ma-check ang update. " +
                                (e.message ?: "Subukan muli mamaya.")
                        } finally {
                            isCheckingUpdate = false
                        }
                    }
                },
                enabled = !isCheckingUpdate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Checking for updates...")
                } else {
                    Text("Check for Updates")
                }
            }

            updateMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://janisxyz.github.io/esp-flasher/"),
                        ),
                    )
                },
            ) {
                Text(t.privacyPolicy)
            }

            Text(
                t.settingsStayOnPhone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = {
                availableUpdate = null
            },
            title = {
                Text("Update Available")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(update.releaseName)
                    Text("Version: ${update.version}")
                    Text(update.releaseNotes)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        availableUpdate = null
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        availableUpdate = null
                    },
                ) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun AccentDot(
    selected: Boolean,
    label: String,
    dynamic: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (dynamic) {
                        Modifier.background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFC51A4A),
                                    Color(0xFF3F51B5),
                                    Color(0xFF0F7A7A),
                                    Color(0xFFF5C518),
                                    Color(0xFFC51A4A),
                                ),
                            ),
                        )
                    } else {
                        Modifier.background(color)
                    },
                )
                .border(
                    if (selected) 3.dp else 1.dp,
                    border,
                    CircleShape,
                ),
        )

        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
