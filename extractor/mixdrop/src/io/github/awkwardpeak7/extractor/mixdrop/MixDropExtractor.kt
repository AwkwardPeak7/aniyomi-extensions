package io.github.awkwardpeak7.extractor.mixdrop

import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.common.extractor.Extractor
import io.github.awkwardpeak7.lib.unpacker.Unpacker
import io.github.awkwardpeak7.network.get
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLDecoder

object MixDropExtractor : Extractor() {

    override fun headersBuilder() = super.headersBuilder()
        .set("Referer", "https://mixdrop.co/")

    override fun supports(data: ExtractableVideo): Boolean {
        return data.url.toHttpUrl().host.contains(mixdropHostRegex)
    }

    private val mixdropHostRegex = Regex("""mixdro+p""")

    override suspend fun extractVideos(data: ExtractableVideo): List<Video> {
        val document = client.get(data.url, headers).asJsoup()
        val unpacked = document.selectFirst("script:containsData(eval):containsData(MDCore)")
            ?.data()
            ?.let(Unpacker::unpack)
            ?: return emptyList()

        val videoUrl = "https:" + unpacked.substringAfter("Core.wurl=\"")
            .substringBefore("\"")

        val subs = unpacked.substringAfter("Core.remotesub=\"").substringBefore('"')
            .takeIf(String::isNotBlank)
            ?.let { listOf(Track(URLDecoder.decode(it, "utf-8"), "sub")) }
            ?: emptyList()

        val quality = buildString {
            append("MixDrop")
            if (!data.quality.isNullOrBlank()) append("(${data.quality})")
        }

        return listOf(
            Video(
                videoUrl,
                quality,
                videoUrl,
                headers,
                subs,
            ),
        )
    }
}
