package org.chronopolis.replicate.batch.rsync

/**
 * A class to denote whether an operation passed or failed (similar to a Try). Pretty similar to
 * what is done in Plaid... not saying I got the idea from there or anything :)
 *
 * Food for thought: should we have any helper methods (map, run) which we can use to apply
 * functions on the event of success? Alternatively, should we look into using a more well defined
 * version of this such as Try, or even the new kotlin.Result from 1.3?
 *
 * @author shake
 */
sealed class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error<out T : Any>(val exception: Exception) : Result<T>()
}

/**
 * Extension function to make a Sequence<Result<T>> into a Result<List<T>>
 */
fun <T : Any> Sequence<Result<T>>.asResult(): Result<List<T>> {
    val mutResult = Result.Success<MutableList<T>>(mutableListOf())
    for (result in this) {
        when (result) {
            is Result.Success -> mutResult.data.add(result.data)
            is Result.Error -> return Result.Error(result.exception) // short circuit
        }
    }
    return mutResult
}

