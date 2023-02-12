package bot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

abstract class Instruction(
    val id: Int,
    var executionTime: Long,
    var possibleDelay: Long,
    val priority: Int,
    val timeout: Long,
    val description: String,
    val onExecuted: (Instruction) -> Unit,
    val onException: (Exception, Instruction) -> Unit = { _, _ -> }
): CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.IO

    var working = false

    abstract suspend fun run()

    suspend fun execute(
        onTimeout: (Instruction) -> Unit,
        onCanceled: (Instruction) -> Unit,
        onExecuted: (Instruction) -> Unit,
        onException: (Exception, Instruction) -> Unit
    ) {
        coroutineScope {
            launch {
                try {
                    withTimeout(timeout) {
                        working = true
                        println("run()")
                        run()
                        if (isActive) {
                            println("isActive")
                            onExecuted(this@Instruction)
                        }
                        println("isActive = false")
                    }
                } catch (e: TimeoutCancellationException) {
                    println("TimeoutCancellationException")
                    onTimeout(this@Instruction)
                } catch (e: CancellationException) {
                    println("CancellationException")
                    onCanceled(this@Instruction)
                } catch (e: Exception) {
                    println("Exception")
                    onException(e, this@Instruction)
                } finally {
                    println("finally")
                    working = false
                }
            }
        }
    }

    fun cancelExecution() {
        cancel()
    }
}