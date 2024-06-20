package io.github.awkwardpeak7.extractor.streamwish

import dev.datlag.jsunpacker.JsUnpacker
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.common.extractor.Extractor
import io.github.awkwardpeak7.lib.playlistutils.PlaylistUtils
import io.github.awkwardpeak7.network.get
import okhttp3.HttpUrl.Companion.toHttpUrl

object StreamWishExtractor : Extractor() {

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    override fun supports(data: ExtractableVideo): Boolean {
        return data.extra?.contains("SB") ?: false ||
            data.extra?.contains("SW") ?: false ||
            data.url.toHttpUrl().host.contains("wish")
    }

    override suspend fun extractVideos(data: ExtractableVideo): List<Video> {
        val document = client.get(data.url, headers).asJsoup()
        val scriptBody = document.selectFirst("script:containsData(m3u8)")?.data()
            ?.let { script ->
                if (script.contains("eval(function(p,a,c")) {
                    JsUnpacker.unpackAndCombine(script)
                } else {
                    script
                }
            }
        val masterUrl = scriptBody
            ?.substringAfter("source", "")
            ?.substringAfter("file:\"", "")
            ?.substringBefore("\"", "")
            ?.takeIf(String::isNotBlank)
            ?: return emptyList()

        return playlistUtils.extractFromHls(masterUrl, data.url, videoNameGen = { quality -> "StreamWish - $quality" })
    }
}
