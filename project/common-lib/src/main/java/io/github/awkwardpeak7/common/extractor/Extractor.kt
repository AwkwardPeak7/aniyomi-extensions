package io.github.awkwardpeak7.common.extractor

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.Headers
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class Extractor {

    private val network: NetworkHelper = Injekt.get<NetworkHelper>()

    protected val client: OkHttpClient = network.client

    protected open fun headersBuilder(): Headers.Builder {
        return Headers.Builder()
            .set("User-Agent", network.defaultUserAgentProvider())
    }

    val headers: Headers by lazy {
        headersBuilder().build()
    }

    abstract suspend fun extractVideos(data: ExtractableVideo): List<Video>

    suspend fun extractVideos(url: String): List<Video> {
        return extractVideos(ExtractableVideo(url))
    }

    abstract fun supports(data: ExtractableVideo): Boolean

    fun supports(url: String): Boolean {
        return supports(ExtractableVideo(url))
    }
}

data class ExtractableVideo(
    val url: String,
    val extra: String? = null,
    val quality: String? = null,
)
