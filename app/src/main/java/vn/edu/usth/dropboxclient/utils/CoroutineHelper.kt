package vn.edu.usth.dropboxclient.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper cho Coroutines - dễ dàng chuyển đổi từ Executor
 */
object CoroutineHelper {

    /**
     * Execute task trên IO thread, return result trên Main thread
     */
    fun <T> executeAsync(
        scope: CoroutineScope,
        onBackground: suspend () -> T,
        onSuccess: (T) -> Unit,
        onError: (Exception) -> Unit
    ): Job {
        return scope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    onBackground()
                }
                onSuccess(result)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Execute task trên IO thread (không cần return)
     */
    fun execute(
        scope: CoroutineScope,
        onBackground: suspend () -> Unit,
        onComplete: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ): Job {
        return scope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    onBackground()
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }
}