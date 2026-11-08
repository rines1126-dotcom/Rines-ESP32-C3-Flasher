
package dev.espflasher.app.ui

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.espflasher.app.UpdateInfo
import dev.espflasher.app.UpdateManager
import dev.espflasher.app.domain.prefs.ThemeMode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: FlasherViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.setLanguage("en")

        if (state.themeMode == ThemeMode.SYSTEM) {
            vm.setThemeMode(ThemeMode.LIGHT)
        }
    }

    val currentVersion = remember {
        @Suppress("DEPRECATION")
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "Unknown"
    }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    fun launchInstaller(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
                )
                clipData = ClipData.newRawUri("APK Update", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            updateMessage =
                "Unable to open APK installer: ${e.message}"
        }
    }

    val installPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            val apkFile = downloadedApk

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.canRequestPackageInstalls()
            ) {
                if (apkFile != null && apkFile.exists()) {
                    launchInstaller(apkFile)
                }
            } else {
                updateMessage =
                    "Please allow Install unknown apps, then try installing again."
            }
        }

    fun installDownloadedApk(apkFile: File) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            downloadedApk = apkFile

            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )

            installPermissionLauncher.launch(settingsIntent)
        } else {
            launchInstaller(apkFile)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
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
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
            )

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = {
                        vm.setThemeMode(ThemeMode.LIGHT)
                    },
                    label = { Text("Light") },
                )

                FilterChip(
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = {
                        vm.setThemeMode(ThemeMode.DARK)
                    },
                    label = { Text("Dark") },
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Keep Screen Awake")
                Switch(
                    checked = state.keepAwake,
                    onCheckedChange = {
                        vm.setKeepAwake(it)
                    },
                )
            }

            HorizontalDivider()

            Text(
                "App Updates",
                style = MaterialTheme.typography.titleSmall,
            )

            Text(
                "Current Version: $currentVersion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    "You're using the latest version ($currentVersion)."
                            }
                        } catch (e: Exception) {
                            updateMessage =
                                "Unable to check for updates. Please try again later."
                        } finally {
                            isCheckingUpdate = false
                        }
                    }
                },
                enabled = !isCheckingUpdate && !isDownloading,
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

            HorizontalDivider()

            Text(
                "Support & Information",
                style = MaterialTheme.typography.titleSmall,
            )

            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://github.com/rines1126-dotcom/Rines-ESP32-C3-Flasher/blob/main/docs/privacy-policy/index.html"
                            ),
                        ),
                    )
                },
            ) {
                Text("Privacy Policy")
            }

            Text(
                "Your settings are saved on this device.",
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
                    Text("Current Version: $currentVersion")
                    Text("Latest Version: ${update.version}")
                    Text("Release Notes:")
                    Text(update.releaseNotes)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloading,
                    onClick = {
                        availableUpdate = null

                        scope.launch {
                            isDownloading = true
                            downloadProgress = 0
                            updateMessage = null

                            try {
                                val apkFile =
                                    UpdateManager.downloadUpdate(
                                        context,
                                        update.apkUrl
                                    ) { progress ->
                                        downloadProgress = progress
                                    }

                                downloadProgress = 100
                                downloadedApk = apkFile
                                installDownloadedApk(apkFile)

                            } catch (e: Exception) {
                                updateMessage =
                                    "APK download failed: ${e.message}"
                            } finally {
                                isDownloading = false
                            }
                        }
                    },
                ) {
                    Text("Download and Install")
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

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Downloading Update")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LinearProgressIndicator(
                        progress = downloadProgress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text("$downloadProgress%")
                    Text("Please wait while the APK is downloading.")
                }
            },
            confirmButton = {},
        )
    }
}
