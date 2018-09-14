package org.chronopolis.rest.api

import okhttp3.RequestBody
import org.chronopolis.rest.models.File
import org.chronopolis.rest.models.Fixity
import org.chronopolis.rest.models.create.FileCreate
import org.chronopolis.rest.models.create.FixityCreate
import org.chronopolis.rest.models.page.SpringPage
import retrofit2.Call
import retrofit2.http.*

/**
 * Client API for File endpoints
 *
 * @author shake
 */
interface FileService {

    @GET("/api/files")
    fun get(@QueryMap params: Map<String, String>): Call<SpringPage<File>>

    @GET("/api/files/{id}")
    fun get(@Path("id") id: Long): Call<File>

    @GET("/api/bags/{id}/files")
    fun get(@Path("bag_id") id: Long, @QueryMap params: Map<String, String>): Call<SpringPage<File>>

    @GET("/api/bags/{bag_id}/files/{file_id}")
    fun get(@Path("bag_id") bagId: Long, @Path("file_id") fileId: Long): Call<File>

    @GET("/api/bags/{bag_id}/files/{file_id}/fixity")
    fun getFixities(@Path("bag_id") bagId: Long, @Path("file_id") fileId: Long): Call<Set<Fixity>>

    @GET("/api/bags/{bag_id}/files/{file_id}/fixity/{algorithm}")
    fun getFixity(@Path("bag_id") bagId: Long,
                  @Path("file_id") fileId: Long,
                  @Path("algorithm") algorithm: String): Call<Fixity>

    @POST("/api/bags/{bag_id}/files")
    fun create(@Path("bag_id") id: Long, @Body create: FileCreate): Call<File>

    @Multipart
    @POST("/api/bags/{bag_id}/files")
    fun createBatch(@Path("bag_id") id: Long, @Part("file") file: RequestBody): Call<Void>

    @PUT("/api/bags/{bag_id}/files/{file_id}/fixity")
    fun createFixity(@Path("bag_id") id: Long, @Path("file_id") fileId: Long, @Body create: FixityCreate): Call<Fixity>
}