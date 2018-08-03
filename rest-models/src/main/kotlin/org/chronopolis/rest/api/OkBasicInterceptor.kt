package org.chronopolis.rest.api

import com.google.common.io.BaseEncoding
import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.StandardCharsets

/**
 * BasicAuth interceptor for OkHttp clients
 *
 * @author shake
 */
class OkBasicInterceptor(val username: String, val password: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = "$username:$password"
        val basicAuth = "Basic " + BaseEncoding.base64()
                .encode(credentials.toByteArray(StandardCharsets.UTF_8))

        val request = chain.request().newBuilder().header("Authorization", basicAuth).build()
        return chain.proceed(request)
    }
}