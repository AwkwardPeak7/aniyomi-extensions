package io.github.awkwardpeak7.lib.javcoverinterceptor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import kotlin.math.ceil

object JavCoverInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val fragment = request.url.fragment
            ?: return response

        if (!fragment.startsWith(FRAG)) {
            return response
        }

        val img = BitmapFactory.decodeStream(response.body.byteStream())

        // 2.11 -> magic number from PythonCoverCrop
        // https://pastebin.com/rZGHz3QW
        val width = ceil(img.width / 2.11).toInt()

        val newImg = Bitmap.createBitmap(img, img.width - width, 0, width, img.height)
        val body = Buffer().run {
            newImg.compress(Bitmap.CompressFormat.JPEG, 90, outputStream())
            asResponseBody("image/jpg".toMediaType())
        }

        return response.newBuilder()
            .body(body)
            .build()
    }

    fun createThumbnail(url: String): String {
        return url.toHttpUrl().newBuilder()
            .fragment(FRAG)
            .build()
            .toString()
    }

    private const val FRAG = "JavCoverInterceptor_"
}
