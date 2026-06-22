package world.anhgelus.parismobility.data

abstract class Result<out R, out E> {
    private val success: Boolean
    private val value: R?
    private val error: E?

    private constructor(
        success: Boolean,
        value: R? = null,
        error: E? = null,
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

    fun onError(action: (E) -> Unit): Result<R, E> {
        if (!success) action(error!!)
        return this
    }

    data class Ok<out R, out E>(val value: R) : Result<R, E>(true, value) {
        override suspend fun <T> map(transform: suspend (R) -> T): Result<T, E> {
            return Ok(transform(value))
        }
    }

    data class Error<out R, out E>(val error: E) : Result<R, E>(false, error = error) {
        override suspend fun <T> map(transform: suspend (R) -> T): Result<T, E> {
            return Error(error)
        }
    }
}

enum class NetworkError(
    val displayError: String
) {
    SERVER_ERROR("Serverside error"),
    INVALID_AUTH("Cannot login"),
    INVALID_DATA("Data sent is invalid"),
    RATE_LIMITED("Rate limited by the server"),
    NO_INTERNET("No internet available"),
    UNKNOWN_ERROR("Unknown error"),
}