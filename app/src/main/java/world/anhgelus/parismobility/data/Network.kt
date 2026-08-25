package world.anhgelus.parismobility.data

abstract class Result<out R, out E> {
    private val success: Boolean
    private val value: R?
    private val error: Pair<E, String?>?

    private constructor(
        success: Boolean,
        value: R? = null,
        error: Pair<E, String?>? = null,
    ) {
        this.success = success
        this.value = value
        this.error = error
    }

    abstract suspend fun <T> map(transform: suspend (R) -> T): Result<T, E>

    fun onSuccess(action: (R) -> Unit): Result<R, E> {
        if (success) action(value!!)
        return this
    }

    fun onError(action: (E, String?) -> Unit): Result<R, E> {
        if (!success) action(error!!.first, error.second)
        return this
    }

    data class Ok<out R, out E>(val value: R) : Result<R, E>(true, value) {
        override suspend fun <T> map(transform: suspend (R) -> T): Result<T, E> {
            return Ok(transform(value))
        }
    }

    data class Error<out R, out E>(val error: E, val data: String? = null) :
        Result<R, E>(false, error = Pair(error, data)) {
        override suspend fun <T> map(transform: suspend (R) -> T): Result<T, E> {
            return Error(error, data)
        }
    }
}

enum class NetworkError(
    val displayError: String
) {
    SERVER_ERROR("Serverside error"),
    INVALID_DATA("Data sent is invalid"),
    RATE_LIMITED("Rate limited by the server"),
    NOT_CONNECTED("Not connected to the server. Check your internet connection"),
    UNKNOWN_ERROR("Unknown error"),
}