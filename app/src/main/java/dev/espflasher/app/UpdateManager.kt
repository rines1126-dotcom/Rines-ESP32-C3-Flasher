
package dev.espflasher.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String
)

object UpdateManager {

    private const val RELEASE_URL =
        "https://api.github.com/repos/rines1126-dotcom/Rines-ESP32-C3-Flasher/releases/latest"

    suspend fun checkForUpdate(context: Context): UpdateInfo? =
        withContext(Dispatchers.IO) {

            val connection =
                URL(RELEASE_URL).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )
                connection.setRequestProperty(
                    "User-Agent",
                    "Rines-ESP32-C3-Flasher"
                )

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("GitHub error: ${connection.responseCode}")
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val release = JSONObject(response)

                if (release.optBoolean("draft", false) ||
                    release.optBoolean("prerelease", false)
                ) {
                    return@withContext null
                }

                val releaseName = release.optString("name", "")
                val tagName = release.optString("tag_name", "")

                val versionPattern = Regex("""\d+\.\d+(?:\.\d+)*""")

                val version = versionPattern.find(releaseName)?.value
                    ?: versionPattern.find(tagName)?.value
                    ?: throw Exception("Invalid release version.")

                val currentVersion = getCurrentVersion(context)

                if (!isNewerVersion(version, currentVersion)) {
                    return@withContext null
                }

                val assets = release.optJSONArray("assets")
                    ?: throw Exception("No release assets found.")

                var apkUrl: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")

                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }

                if (apkUrl.isNullOrBlank()) {
                    throw Exception("No APK found in the latest release.")
                }

                UpdateInfo(
                    version = version,
                    releaseName = releaseName.ifBlank { "Version $version" },
                    releaseNotes = release.optString(
                        "body",
                        "No release notes."
                    ),
                    apkUrl = apkUrl
                )

            } finally {
                connection.disconnect()
            }
        }

    suspend fun downloadUpdate(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {

        val updateDirectory = File(context.cacheDir, "updates")

        if (!updateDirectory.exists() && !updateDirectory.mkdirs()) {
            throw Exception("Unable to create update folder.")
        }

        val temporaryFile = File(updateDirectory, "update.apk.part")
        val apkFile = File(updateDirectory, "update.apk")

        val connection =
            URL(apkUrl).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.setRequestProperty(
                "User-Agent",
                "Rines-ESP32-C3-Flasher"
            )

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception(
                    "Download failed: HTTP ${connection.responseCode}"
                )
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(temporaryFile).use { output ->

                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also {
                            bytesRead = it
                        } != -1
                    ) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val progress =
                                ((downloadedBytes * 100) / totalBytes)
                                    .toInt()
                                    .coerceIn(0, 100)

                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }

                    output.flush()
                }
            }

            if (!temporaryFile.exists() || temporaryFile.length() == 0L) {
                throw Exception("Downloaded APK is empty.")
            }

            if (apkFile.exists()) {
                apkFile.delete()
            }

            if (!temporaryFile.renameTo(apkFile)) {
                temporaryFile.copyTo(apkFile, overwrite = true)
                temporaryFile.delete()
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
            }

            apkFile

        } catch (e: Exception) {
            temporaryFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun getCurrentVersion(context: Context): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "0.0.0"
    }

    private fun isNewerVersion(
        latest: String,
        current: String
    ): Boolean {
        val latestParts = latest.split(".").map {
            it.toIntOrNull() ?: 0
        }

        val currentParts = current.split(".").map {
            it.toIntOrNull() ?: 0
        }

        val count = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until count) {
            val latestNumber = latestParts.getOrElse(i) { 0 }
            val currentNumber = currentParts.getOrElse(i) { 0 }

            if (latestNumber > currentNumber) return true
            if (latestNumber < currentNumber) return false
        }

        return false
    }
}
