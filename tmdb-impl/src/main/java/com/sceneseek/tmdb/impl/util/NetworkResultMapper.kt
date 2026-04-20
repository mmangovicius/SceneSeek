package com.sceneseek.tmdb.impl.util

import com.sceneseek.core.domain.util.Result
import retrofit2.Response
import java.io.IOException

class AuthException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class ServerException(val code: Int) : Exception("Server error: $code")
class NetworkException(cause: Throwable) : Exception("Network error", cause)

fun <T> Response<T>.toResult(): Result<T> {
    return try {
        when {
            isSuccessful && body() != null -> Result.Success(body()!!)
            code() == 401 -> Result.Error(AuthException("Unauthorized — check API key"))
            code() == 404 -> Result.Error(NotFoundException("Resource not found"))
            code() >= 500 -> Result.Error(ServerException(code()))
            else -> Result.Error(Exception("HTTP error: ${code()}"))
        }
    } catch (e: IOException) {
        Result.Error(NetworkException(e))
    }
}
