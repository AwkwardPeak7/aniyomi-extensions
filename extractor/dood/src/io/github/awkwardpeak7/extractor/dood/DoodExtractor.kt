package io.github.awkwardpeak7.extractor.dood

import eu.kanade.tachiyomi.animesource.model.Video
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.common.extractor.Extractor
import io.github.awkwardpeak7.network.get
import okhttp3.HttpUrl.Companion.toHttpUrl

object DoodExtractor : Extractor() {

    override fun supports(data: ExtractableVideo): Boolean {
        val host = data.url.toHttpUrl().host

        return host.contains(doodhostRegex) ||
            doodhosts.any { host.contains(it) } ||
            data.extra?.contains("DD") ?: true
    }

    private val doodhostRegex = Regex("do+d")
    private val doodhosts = listOf(
        "ds2play",
    )

    override suspend fun extractVideos(data: ExtractableVideo): List<Video> {
        val quality = "Doodstream"
        val response = client.get(data.url, headers)
        val url = response.request.url
        val content = response.body.string()
        if (!content.contains("'/pass_md5/")) return emptyList()

        val md5 = content.substringAfter("'/pass_md5/").substringBefore("',")
        val token = md5.substringAfterLast("/")
        val expiry = System.currentTimeMillis()
        val videoUrlStart = client.get(
            "https://${url.host}/pass_md5/$md5",
            headersBuilder().set("referer", url.toString()).build(),
        ).body.string()
        val videoUrl = "$videoUrlStart${getRandomString()}?token=$token&expiry=$expiry"

        return listOf(
            Video(
                url.toString(),
                quality,
                videoUrl,
                headers = doodHeaders(url.host),
            ),
        )
    }

    private fun getRandomString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun doodHeaders(host: String) = headersBuilder()
        .add("Referer", "https://$host/")
        .build()
}
