package org.chronopolis.rest.api

import org.chronopolis.rest.api.Paths.BAG_ROOT
import org.chronopolis.rest.api.Paths.TOKEN_ROOT
import org.chronopolis.rest.models.AceToken
import org.chronopolis.rest.models.create.AceTokenCreate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * Ingest API operations on ACE Tokens
 *
 * @author shake
 */
interface TokenService {

    @GET(TOKEN_ROOT)
    fun getTokens(@QueryMap params: Map<String, String>): Call<Iterable<AceToken>>

    @GET("$TOKEN_ROOT/{id}")
    fun getToken(@Path("id") id: Long): Call<AceToken>

    @GET("$BAG_ROOT/{bagId}/tokens")
    fun getBagTokens(@Path("bagId") bagId: Long,
                     @QueryMap params: Map<String, String>): Call<Iterable<AceToken>>

    @POST("$BAG_ROOT/{bagId}/tokens")
    fun createToken(@Path("bagId") bagId: Long, @Body model: AceTokenCreate): Call<AceToken>

}