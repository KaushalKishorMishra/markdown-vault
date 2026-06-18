package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GitHubTreeEntry>,
    val truncated: Boolean
)

@JsonClass(generateAdapter = true)
data class GitHubTreeEntry(
    val path: String,
    val mode: String,
    val type: String, // "blob" or "tree"
    val sha: String,
    val size: Int = 0,
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubFileContentResponse(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val content: String?, // Base64 encoded
    val encoding: String?
)

@JsonClass(generateAdapter = true)
data class UpdateFileRequest(
    val message: String,
    val content: String, // Base64 encoded
    val sha: String? = null, // required if updating
    val branch: String
)

@JsonClass(generateAdapter = true)
data class UpdateFileResponse(
    val content: GitHubFileContentResponse?,
    val commit: GitHubCommitInfo
)

@JsonClass(generateAdapter = true)
data class DeleteFileRequest(
    val message: String,
    val sha: String, // required
    val branch: String
)

@JsonClass(generateAdapter = true)
data class GitHubCommitInfo(
    val sha: String,
    val message: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val login: String,
    val name: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val email: String?
)
