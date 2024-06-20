package io.github.awkwardpeak7.extractor.emturbovid

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.common.extractor.Extractor
import io.github.awkwardpeak7.lib.playlistutils.PlaylistUtils
import io.github.awkwardpeak7.network.get
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object EmTurboVidExtractor : Extractor() {

    private val playlistExtractor by lazy { PlaylistUtils(client) }

    override suspend fun extractVideos(data: ExtractableVideo): List<Video> {
        val document = client.get(data.url, headers).asJsoup()

        val script = document.selectFirst("script:containsData(urlplay)")
            ?.data()
            ?: return emptyList()

        val urlPlay = urlPlay.find(script)?.groupValues?.get(1)
            ?: return emptyList()

        if (urlPlay.toHttpUrlOrNull() == null) {
            return emptyList()
        }

        return playlistExtractor.extractFromHls(
            urlPlay,
            data.url,
            videoNameGen = { quality -> "EmTurboVid: $quality" },
        ).distinctBy { it.url }
    }

    override fun supports(data: ExtractableVideo): Boolean {
        val host = data.url.toHttpUrl().host

        return host.contains("emturbo") && data.extra?.contains("TV") ?: true
    }

    private val urlPlay = Regex("""urlPlay\s*=\s*\'([^\']+)""")
}
