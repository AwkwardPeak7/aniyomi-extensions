package io.github.awkwardpeak7.extractor.streamtape

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.common.extractor.Extractor
import io.github.awkwardpeak7.network.get
import okhttp3.HttpUrl.Companion.toHttpUrl

object StreamTapeExtractor : Extractor() {

    override fun supports(data: ExtractableVideo): Boolean {
        return data.url.toHttpUrl().host.contains("streamtape")
    }

    override suspend fun extractVideos(data: ExtractableVideo): List<Video> {
        val baseUrl = "https://streamtape.com/e/"
        val newUrl = if (!data.url.startsWith(baseUrl)) {
            // ["https", "", "<domain>", "<???>", "<id>", ...]
            val id = data.url.split("/").getOrNull(4) ?: return emptyList()
            baseUrl + id
        } else { data.url }

        val document = client.get(newUrl, headers).asJsoup()
        val targetLine = "document.getElementById('robotlink')"
        val script = document.selectFirst("script:containsData($targetLine)")
            ?.data()
            ?.substringAfter("$targetLine.innerHTML = '")
            ?: return emptyList()
        val videoUrl = "https:" + script.substringBefore("'") +
            script.substringAfter("+ ('xcd").substringBefore("'")

        val cookie = client.cookieJar.loadForRequest("https://streamtape.com".toHttpUrl())
            .firstOrNull { it.name == "cf_clearance" }

        val videoHeaders = cookie?.let {
            headersBuilder()
                .set("Cookie", "${it.name}=${it.value}")
                .build()
        } ?: headers

        return listOf(
            Video(
                videoUrl,
                data.quality ?: "Streamtape",
                videoUrl,
                videoHeaders,
            ),
        )
    }
}
