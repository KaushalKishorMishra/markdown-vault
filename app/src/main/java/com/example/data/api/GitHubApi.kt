package com.example.data.api

import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") authHeader: String
    ): Response<UserResponse>

    @GET("repos/{owner}/{repo}/git/trees/{branch}")
    suspend fun getTree(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Query("recursive") recursive: Int = 1
    ): Response<GitHubTreeResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String
    ): Response<GitHubFileContentResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: UpdateFileRequest
    ): Response<UpdateFileResponse>

    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: DeleteFileRequest
    ): Response<Unit>
}
