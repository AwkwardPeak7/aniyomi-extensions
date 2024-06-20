package io.github.awkwardpeak7.network

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response

suspend inline fun OkHttpClient.get(
    url: String,
    headers: Headers,
): Response {
    return newCall(GET(url, headers)).awaitSuccess()
}

suspend inline fun OkHttpClient.getNotChecking(
    url: String,
    headers: Headers,
): Response {
    return newCall(GET(url, headers)).await()
}

suspend inline fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers,
): Response {
    return newCall(GET(url, headers)).awaitSuccess()
}

suspend inline fun OkHttpClient.post(
    url: String,
    headers: Headers,
    body: RequestBody = FormBody.Builder().build(),
): Response {
    return newCall(POST(url, headers, body)).awaitSuccess()
}
