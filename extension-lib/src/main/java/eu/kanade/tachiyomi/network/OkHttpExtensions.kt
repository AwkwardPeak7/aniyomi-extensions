package eu.kanade.tachiyomi.network

import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import kotlin.time.Duration.Companion.minutes

suspend fun Call.await(): Response = throw Exception("Stub!")

/**
 * @since extensions-lib 14
 */
suspend fun Call.awaitSuccess(): Response = throw Exception("Stub!")

/**
 * Exception that handles HTTP codes considered not successful by OkHttp.
 * Use it to have a standardized error message in the app across the extensions.
 *
 * @since extensions-lib 14
 * @param code [Int] the HTTP status code
 */
class HttpException(val code: Int) : IllegalStateException("HTTP error $code")

suspend inline fun OkHttpClient.get(
    url: String,
    headers: Headers,
    cache: CacheControl = CacheControl.Builder().maxAge(10.minutes).build()
): Response {
    return newCall(GET(url, headers, cache)).awaitSuccess()
}

suspend inline fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers,
    cache: CacheControl = CacheControl.Builder().maxAge(10.minutes).build()
): Response {
    return newCall(GET(url, headers, cache)).awaitSuccess()
}

suspend inline fun OkHttpClient.post(
    url: String,
    headers: Headers,
    body: RequestBody = FormBody.Builder().build(),
    cache: CacheControl = CacheControl.Builder().maxAge(10.minutes).build()
): Response {
    return newCall(POST(url, headers, body, cache)).awaitSuccess()
}
