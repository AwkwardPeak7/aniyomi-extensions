package io.github.awkwardpeak7.common.extractor

import eu.kanade.tachiyomi.animesource.model.Video

interface Extractor {

    suspend fun extractVideos(data: ExtractableVideo): List<Video>

    fun supports(data: ExtractableVideo): Boolean
}

data class ExtractableVideo(
    val url: String,
    val label: String? = null,
)