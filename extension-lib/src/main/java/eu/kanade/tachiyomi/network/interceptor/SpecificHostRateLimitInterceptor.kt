package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An OkHttp interceptor that handles given url host's rate limiting.
 *
 * Examples:
 *
 * httpUrl = "https://api.anime.com".toHttpUrl(), permits = 5, period = 1.seconds =>  5 requests per second to api.anime.com
 * httpUrl = "https://cdn.thumbnails.com".toHttpUrl(), permits = 10, period = 2.minutes  =>  10 requests per 2 minutes to cdn.thumbnails.com
 *
 * @since extension-lib 14
 *
 * @param httpUrl [HttpUrl] The url host that this interceptor should handle. Will get url's host by using HttpUrl.host()
 * @param permits [Int]     Number of requests allowed within a period of units.
 * @param period [Duration] The limiting duration. Defaults to 1.seconds.
 */
@Suppress("unused_parameter")
fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = throw Exception("Stub!")

/**
 * An OkHttp interceptor that handles given url host's rate limiting.
 *
 * Examples:
 *
 * url = "https://api.anime.com", permits = 5, period = 1.seconds =>  5 requests per second to api.anime.com
 * url = "https://cdn.thumbnails.com", permits = 10, period = 2.minutes  =>  10 requests per 2 minutes to cdn.thumbnails.com
 *
 * @since extension-lib 14
 *
 * @param url [String]      The url host that this interceptor should handle. Will get url's host by using HttpUrl.host()
 * @param permits [Int]     Number of requests allowed within a period of units.
 * @param period [Duration] The limiting duration. Defaults to 1.seconds.
 */
@Suppress("unused_parameter")
fun OkHttpClient.Builder.rateLimitHost(
    url: String,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = throw Exception("Stub!")
