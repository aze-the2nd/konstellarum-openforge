package de.konstellarum.synesis.update

import de.konstellarum.synesis.AppVersion
import de.konstellarum.synesis.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseUpdateRepository(
    private val owner: String = BuildConfig.UPDATE_REPO_OWNER,
    private val repo: String = BuildConfig.UPDATE_REPO_NAME,
    private val tagPrefix: String = BuildConfig.UPDATE_TAG_PREFIX,
    private val assetPrefix: String = BuildConfig.UPDATE_ASSET_PREFIX,
) : UpdateRepository {
    override suspend fun fetchLatest(): UpdateInfo? {
        val endpoint = URL("https://api.github.com/repos/$owner/$repo/releases?per_page=30")
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "Synesis")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        return try {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseLatestRelease(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLatestRelease(json: String): UpdateInfo? {
        val releases = JSONArray(json)
        for (index in 0 until releases.length()) {
            val release = releases.getJSONObject(index)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue

            val tagName = release.optString("tag_name")
            if (!tagName.startsWith(tagPrefix)) continue

            val version = AppVersion.parse(tagName.removePrefix(tagPrefix)) ?: continue
            val downloadUrl = findAssetUrl(release, version) ?: continue
            val changelog = release.optString("body").takeIf { it.isNotBlank() }
                ?: "Keine Release Notes verfügbar."

            return UpdateInfo(
                version = version,
                tagName = tagName,
                downloadUrl = downloadUrl,
                changelog = changelog,
            )
        }
        return null
    }

    private fun findAssetUrl(release: JSONObject, version: AppVersion): String? {
        val expectedName = "$assetPrefix$version.apk"
        val assets = release.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val assetName = asset.optString("name")
            if (assetName == expectedName) {
                return asset.optString("browser_download_url")
            }
        }
        return null
    }
}
