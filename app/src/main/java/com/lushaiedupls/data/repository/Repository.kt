package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.safeApiCall

/**
 * Base for data-layer repositories. ViewModels call repository methods, never Retrofit directly.
 *
 * ```
 * class AuthRepository(private val authApi: AuthApi) : Repository() {
 *     suspend fun login(email: String, password: String) = apiCall {
 *         authApi.login(LoginRequest(email, password))
 *     }
 * }
 * ```
 */
abstract class Repository {
    protected suspend fun <T> apiCall(block: suspend () -> T): NetworkResult<T> = safeApiCall(block)
}
