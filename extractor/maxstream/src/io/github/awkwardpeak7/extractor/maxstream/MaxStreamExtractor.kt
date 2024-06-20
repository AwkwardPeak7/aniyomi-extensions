package io.github.awkwardpeak7.extractor.maxstream

import dev.datlag.jsunpacker.JsUnpacker
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.common.extractor.Extractor
import io.github.awkwardpeak7.lib.playlistutils.PlaylistUtils
import io.github.awkwardpeak7.network.get
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object MaxStreamExtractor : Extractor() {

    private val playListUtils by lazy { PlaylistUtils(client, headers) }

    override fun supports(data: ExtractableVideo): Boolean {
        return data.url.contains("maxstream")
    }

    override suspend fun extractVideos(data: ExtractableVideo): List<Video> {
        val document = client.get(data.url, headers).asJsoup()

        val script = document.selectFirst("script:containsData(function(p,a,c,k,e,d))")
            ?.data()
            ?.let(JsUnpacker::unpackAndCombine)
            ?: return emptyList()

        val videoUrl = script.substringAfter("file:\"").substringBefore("\"")

        if (videoUrl.toHttpUrlOrNull() == null) {
            return emptyList()
        }

        return playListUtils.extractFromHls(
            videoUrl,
            data.url,
            videoNameGen = { quality -> "MaxStream: $quality" },
        )
    }
}
