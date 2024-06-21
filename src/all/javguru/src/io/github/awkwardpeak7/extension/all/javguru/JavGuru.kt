package io.github.awkwardpeak7.extension.all.javguru

import android.app.Application
import android.util.Base64
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.parallelCatchingFlatMap
import io.github.awkwardpeak7.common.extractor.ExtractableVideo
import io.github.awkwardpeak7.extractor.dood.DoodExtractor
import io.github.awkwardpeak7.extractor.emturbovid.EmTurboVidExtractor
import io.github.awkwardpeak7.extractor.maxstream.MaxStreamExtractor
import io.github.awkwardpeak7.extractor.mixdrop.MixDropExtractor
import io.github.awkwardpeak7.extractor.streamtape.StreamTapeExtractor
import io.github.awkwardpeak7.extractor.streamwish.StreamWishExtractor
import io.github.awkwardpeak7.lib.javcoverinterceptor.JavCoverInterceptor
import io.github.awkwardpeak7.network.get
import io.github.awkwardpeak7.network.getNotChecking
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.select.Elements
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.min

class JavGuru : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "Jav Guru"

    override val baseUrl = "https://jav.guru"

    override val lang = "all"

    override val supportsLatest = true

    private val noRedirectClient = network.client.newBuilder()
        .followRedirects(false)
        .build()

    private val preference by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val client = network.client.newBuilder()
        .addNetworkInterceptor(JavCoverInterceptor)
        .build()

    private lateinit var popularElements: Elements

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        return if (page == 1) {
            client.get("$baseUrl/most-watched-rank/", headers)
                .use {
                    popularElements = it.asJsoup().select(".tabcontent li")

                    cachedPopularAnimeParse(1)
                }
        } else {
            cachedPopularAnimeParse(page)
        }
    }

    private fun cachedPopularAnimeParse(page: Int): AnimesPage {
        val end = min(page * 20, popularElements.size)
        val entries = popularElements.subList((page - 1) * 20, end).map { element ->
            SAnime.create().apply {
                element.select("a").let { a ->
                    getIDFromUrl(a)?.let { url = it }
                        ?: setUrlWithoutDomain(a.attr("href"))

                    title = a.text()
                    thumbnail_url = JavCoverInterceptor.createThumbnail(
                        a.select("img").attr("abs:src"),
                    )
                }
            }
        }
        return AnimesPage(entries, end < popularElements.size)
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage {
        val url = baseUrl + if (page > 1) "/page/$page/" else ""

        return client.get(url, headers).use(::animeListParse)
    }

    private fun animeListParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val entries = document.select("div.site-content div.inside-article:not(:contains(nothing))").map { element ->
            SAnime.create().apply {
                element.select("a").let { a ->
                    getIDFromUrl(a)?.let { url = it }
                        ?: setUrlWithoutDomain(a.attr("href"))
                }
                thumbnail_url = JavCoverInterceptor.createThumbnail(
                    element.select("img").attr("abs:src"),
                )
                title = element.select("h2 > a").text()
            }
        }

        val page = document.location()
            .removeSuffix("/")
            .substringAfterLast("/")
            .toIntOrNull() ?: 1

        val lastPage = document.select("div.wp-pagenavi a")
            .last()
            ?.attr("href")
            .pageNumberFromUrlOrNull() ?: 1

        return AnimesPage(entries, page < lastPage)
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        if (query.startsWith(PREFIX_ID)) {
            val id = query.substringAfter(PREFIX_ID)
            if (id.toIntOrNull() == null) {
                return AnimesPage(emptyList(), false)
            }
            val url = "/$id/"
            val tempAnime = SAnime.create().apply { this.url = url }
            return getAnimeDetails(tempAnime).let {
                val anime = it.apply { this.url = url }
                AnimesPage(listOf(anime), false)
            }
        } else if (query.isNotEmpty()) {
            return client.newCall(searchAnimeRequest(page, query))
                .awaitSuccess()
                .use(::animeListParse)
        } else {
            filters.forEach { filter ->
                when (filter) {
                    is TagFilter,
                    is CategoryFilter,
                    -> {
                        if (filter.state != 0) {
                            val url = "$baseUrl${filter.toUrlPart()}" + if (page > 1) "page/$page/" else ""
                            val request = GET(url, headers)
                            return client.newCall(request)
                                .awaitSuccess()
                                .use(::animeListParse)
                        }
                    }
                    is ActressFilter,
                    is ActorFilter,
                    is StudioFilter,
                    is MakerFilter,
                    -> {
                        if ((filter.state as String).isNotEmpty()) {
                            val url = "$baseUrl${filter.toUrlPart()}" + if (page > 1) "page/$page/" else ""
                            val request = GET(url, headers)
                            return client.newCall(request)
                                .awaitIgnoreCode(404)
                                .use(::animeListParse)
                        }
                    }
                    else -> { }
                }
            }
        }

        throw Exception("Select at least one Filter")
    }

    private fun searchAnimeRequest(page: Int, query: String): Request {
        val url = baseUrl.toHttpUrl().newBuilder().apply {
            if (page > 1) addPathSegments("page/$page/")
            addQueryParameter("s", query)
        }.build().toString()

        return GET(url, headers)
    }

    override fun getFilterList() = getFilters()

    override fun getAnimeUrl(anime: SAnime): String {
        return baseUrl + anime.url
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        val url = getAnimeUrl(anime)

        return client.get(url, headers).use { response ->
            val document = response.asJsoup()

            SAnime.create().apply {
                title = document.select(".titl").text()
                genre = document.select(".infoleft a[rel*=tag]").joinToString { it.text() }
                author = document.selectFirst(".infoleft li:contains(studio) a")?.text()
                artist = document.selectFirst(".infoleft li:contains(label) a")?.text()
                status = SAnime.COMPLETED
                description = buildString {
                    document.selectFirst(".infoleft li:contains(code)")?.text()?.let { append("$it\n") }
                    document.selectFirst(".infoleft li:contains(director)")?.text()?.let { append("$it\n") }
                    document.selectFirst(".infoleft li:contains(studio)")?.text()?.let { append("$it\n") }
                    document.selectFirst(".infoleft li:contains(label)")?.text()?.let { append("$it\n") }
                    document.selectFirst(".infoleft li:contains(actor)")?.text()?.let { append("$it\n") }
                    document.selectFirst(".infoleft li:contains(actress)")?.text()?.let { append("$it\n") }
                }
                thumbnail_url = document.selectFirst(".wp-content img[src*=m.media-amazon.com]")?.absUrl("src")
                    ?: JavCoverInterceptor.createThumbnail(
                        document.select(".large-screenshot img").attr("abs:src"),
                    )
            }
        }
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return listOf(
            SEpisode.create().apply {
                url = anime.url
                name = "Episode"
            },
        )
    }

    override fun getEpisodeUrl(episode: SEpisode): String {
        return baseUrl + episode.url
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val url = getEpisodeUrl(episode)

        return client.get(url, headers).use { response ->
            val document = response.asJsoup()

            val iframeData = document.selectFirst("script:containsData(iframe_url)")?.html()
                ?: return emptyList()

            val iframeUrls = IFRAME_B64_REGEX.findAll(iframeData)
                .map { it.groupValues[1] }
                .map { Base64.decode(it, Base64.DEFAULT).let(::String) }
                .toList()

            val iframeHeader = headersBuilder()
                .set("Referer", url)
                .build()

            iframeUrls.mapNotNull { iframeUrl ->
                val iframeDocument = client.getNotChecking(iframeUrl, iframeHeader).asJsoup()

                val script = iframeDocument.selectFirst("script:containsData(start_player)")
                    ?.html() ?: return@mapNotNull null

                val olid = IFRAME_OLID_REGEX.find(script)?.groupValues?.get(1)?.reversed()
                    ?: return@mapNotNull null

                val olidUrl = IFRAME_OLID_URL.find(script)?.groupValues?.get(1)
                    ?.substringBeforeLast("=")?.let { "$it=$olid" }
                    ?: return@mapNotNull null

                val olidHeaders = headersBuilder()
                    .set("Referer", iframeUrl)
                    .build()

                val redirectUrl = noRedirectClient.getNotChecking(olidUrl, olidHeaders)
                    .use { it.header("location") }
                    ?: return@mapNotNull null

                if (redirectUrl.toHttpUrlOrNull() == null) {
                    return@mapNotNull null
                }

                redirectUrl
            }.parallelCatchingFlatMap(::getVideos)
        }.sort()
    }

    private suspend fun getVideos(hosterUrl: String): List<Video> {
        return when {
            // TODO: use ExtractableVideo class to detect
            listOf("javplaya", "javclan").any { it in hosterUrl } -> {
                val data = ExtractableVideo(url = hosterUrl, referer = "$baseUrl/")
                StreamWishExtractor.extractVideos(data)
            }

            StreamTapeExtractor.supports(hosterUrl) -> {
                StreamTapeExtractor.extractVideos(hosterUrl)
            }

            DoodExtractor.supports(hosterUrl) -> {
                DoodExtractor.extractVideos(hosterUrl)
            }

            MixDropExtractor.supports(hosterUrl) -> {
                MixDropExtractor.extractVideos(hosterUrl)
            }

            MaxStreamExtractor.supports(hosterUrl) -> {
                MaxStreamExtractor.extractVideos(hosterUrl)
            }

            EmTurboVidExtractor.supports(hosterUrl) -> {
                EmTurboVidExtractor.extractVideos(hosterUrl)
            }

            else -> emptyList()
        }
    }

    override fun List<Video>.sort(): List<Video> {
        val quality = preference.getString(PREF_QUALITY, PREF_QUALITY_DEFAULT)!!

        return sortedWith(
            compareBy { it.quality.contains(quality) },
        ).reversed()
    }

    private fun getIDFromUrl(element: Elements): String? {
        return element.attr("abs:href")
            .toHttpUrlOrNull()
            ?.pathSegments
            ?.firstOrNull()
            ?.toIntOrNull()
            ?.toString()
            ?.let { "/$it/" }
    }

    private fun String?.pageNumberFromUrlOrNull() =
        this
            ?.substringBeforeLast("/")
            ?.toHttpUrlOrNull()
            ?.pathSegments
            ?.last()
            ?.toIntOrNull()

    private suspend fun Call.awaitIgnoreCode(code: Int): Response {
        return await().also { response ->
            if (!response.isSuccessful && response.code != code) {
                response.close()
                throw Exception("HTTP error ${response.code}")
            }
        }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = PREF_QUALITY
            title = PREF_QUALITY_TITLE
            entries = arrayOf("1080p", "720p", "480p", "360p")
            entryValues = arrayOf("1080", "720", "480", "360")
            setDefaultValue(PREF_QUALITY_DEFAULT)
            summary = "%s"
        }.also(screen::addPreference)
    }

    companion object {
        const val PREFIX_ID = "id:"

        private val IFRAME_B64_REGEX = Regex(""""iframe_url":"([^"]+)"""")
        private val IFRAME_OLID_REGEX = Regex("""var OLID = '([^']+)'""")
        private val IFRAME_OLID_URL = Regex("""src="([^"]+)"""")

        private const val PREF_QUALITY = "preferred_quality"
        private const val PREF_QUALITY_TITLE = "Preferred quality"
        private const val PREF_QUALITY_DEFAULT = "720"
    }
}
