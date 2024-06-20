package io.github.awkwardpeak7.extension.all.missav

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.get
import eu.kanade.tachiyomi.util.asJsoup
import io.github.awkwardpeak7.lib.javcoverfetcher.JavCoverFetcher
import io.github.awkwardpeak7.lib.javcoverfetcher.JavCoverFetcher.fetchHDCovers
import io.github.awkwardpeak7.lib.playlistutils.PlaylistUtils
import io.github.awkwardpeak7.lib.unpacker.Unpacker
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MissAV : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "MissAV"

    override val lang = "all"

    override val baseUrl = "https://missav.com"

    override val supportsLatest = true

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    private val playlistExtractor by lazy {
        PlaylistUtils(client, headers)
    }

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        return client.get("$baseUrl/en/today-hot?page=$page", headers)
            .use(::animeListParse)
    }

    private fun animeListParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val entries = document.select("div.thumbnail").map { element ->
            SAnime.create().apply {
                element.select("a.text-secondary").also {
                    setUrlWithoutDomain(it.attr("href"))
                    title = it.text()
                }
                thumbnail_url = element.selectFirst("img")?.attr("abs:data-src")
            }
        }

        val hasNextPage = document.selectFirst("a[rel=next]") != null

        return AnimesPage(entries, hasNextPage)
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage {
        return client.get("$baseUrl/en/new?page=$page", headers)
            .use(::animeListParse)
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        val url = baseUrl.toHttpUrl().newBuilder().apply {
            val genre = filters.firstInstanceOrNull<GenreList>()?.selected
            if (query.isNotEmpty()) {
                addEncodedPathSegments("en/search")
                addPathSegment(query.trim())
            } else if (genre != null) {
                addEncodedPathSegments(genre)
            } else {
                addEncodedPathSegments("en/new")
            }
            filters.firstInstanceOrNull<SortFilter>()?.selected?.let {
                addQueryParameter("sort", it)
            }
            addQueryParameter("page", page.toString())
        }.build()

        return client.get(url, headers)
            .use(::animeListParse)
    }

    override fun getFilterList() = getFilters()

    override fun getAnimeUrl(anime: SAnime): String {
        return baseUrl + anime.url
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return client.get(getAnimeUrl(anime), headers).use { response ->
            val document = response.asJsoup()

            val jpTitle = document.select("div.text-secondary span:contains(title) + span").text()
            val siteCover = document.selectFirst("video.player")?.attr("abs:data-poster")

            SAnime.create().apply {
                title = document.selectFirst("h1.text-base")!!.text()
                genre = document.getInfo("/genres/")
                author = listOfNotNull(
                    document.getInfo("/directors/"),
                    document.getInfo("/makers/"),
                ).joinToString()
                artist = document.getInfo("/actresses/")
                status = SAnime.COMPLETED
                description = buildString {
                    document.selectFirst("div.mb-1")?.text()?.also { append("$it\n") }

                    document.getInfo("/labels/")?.also { append("\nLabel: $it") }
                    document.getInfo("/series/")?.also { append("\nSeries: $it") }

                    document.select("div.text-secondary:not(:has(a)):has(span)")
                        .eachText()
                        .forEach { append("\n$it") }
                }
                thumbnail_url = if (preferences.fetchHDCovers) {
                    JavCoverFetcher.getCoverByTitle(jpTitle) ?: siteCover
                } else {
                    siteCover
                }
            }
        }
    }

    private fun Element.getInfo(urlPart: String) =
        select("div.text-secondary > a[href*=$urlPart]")
            .eachText()
            .joinToString()
            .takeIf(String::isNotBlank)

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
        return client.get(getEpisodeUrl(episode), headers).use {
            val document = it.asJsoup()

            val playlists = document.selectFirst("script:containsData(function(p,a,c,k,e,d))")
                ?.data()
                ?.let(Unpacker::unpack)?.ifEmpty { null }
                ?: return emptyList()

            val masterPlaylist = playlists.substringAfter("source=\"").substringBefore("\";")

            playlistExtractor.extractFromHls(masterPlaylist, referer = "$baseUrl/")
        }.sort()
    }

    override fun List<Video>.sort(): List<Video> {
        val quality = preferences.getString(PREF_QUALITY, PREF_QUALITY_DEFAULT)!!

        return sortedWith(
            compareBy { it.quality.contains(quality) },
        ).reversed()
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = PREF_QUALITY
            title = PREF_QUALITY_TITLE
            entries = arrayOf("720p", "480p", "360p")
            entryValues = arrayOf("720", "480", "360")
            setDefaultValue(PREF_QUALITY_DEFAULT)
            summary = "%s"
        }.also(screen::addPreference)

        JavCoverFetcher.addPreferenceToScreen(screen)
    }

    private inline fun <reified T> List<*>.firstInstanceOrNull(): T? =
        filterIsInstance<T>().firstOrNull()

    companion object {
        private const val PREF_QUALITY = "preferred_quality"
        private const val PREF_QUALITY_TITLE = "Preferred quality"
        private const val PREF_QUALITY_DEFAULT = "720"
    }
}
