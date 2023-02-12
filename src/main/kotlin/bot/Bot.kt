package bot

import LOGGER_LEVEL
import domain.repositories.ConfigurationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import utils.CloseableCoroutineScope
import utils.Logger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class Bot(
    scope: CoroutineScope,
    configs: ConfigurationsRepository,
    private val onInstructionTimeout: (Exception) -> Unit,
    private val onInstructionFailed: (Exception, Instruction) -> Unit
) {

    private val botScope: CloseableCoroutineScope

    private val instructionList = InstructionList(::onListChanged)
    private var started = false
    private var working = false
    private var instructionExecutionJob: Job

    private val log = Logger(this.javaClass.name, configs, LOGGER_LEVEL)

    init {
        botScope = CloseableCoroutineScope(Dispatchers.IO + scope.coroutineContext + SupervisorJob())
        instructionExecutionJob = Job(botScope.coroutineContext.job)
        instructionExecutionJob.cancel()
    }

    fun isAnyInstructionExecuting(): Boolean {
        return if (instructionList.list.isEmpty()) {
            instructionExecutionJob.isActive
        } else {
            false
        }
    }

    val isPause: Boolean
        get() = !started

    fun add(instruction: Instruction) {
        instructionList.add(instruction)
    }

    fun addAll(instructions: List<Instruction>) {
        instructionList.addAll(instructions)
    }

    fun remove(instruction: Instruction) {
        instructionList.remove(instruction)
    }

    fun remove(id: Int) {
        instructionList.remove(id)
    }

    fun getInstructions(): List<Instruction> {
        return instructionList.list
    }

    fun start() {
        if (started.not()) {
            started = true
            setupNextInstruction()
        }
    }

    fun stop(cancelInstructionExecution: Boolean) {
        started = false
        if (cancelInstructionExecution) cancelExecution()
    }

    fun cancelExecution() {
        if (instructionExecutionJob.isActive) {
            instructionExecutionJob.cancel()
            if (instructionList.isEmpty().not() && instructionList.first().working) {
                instructionList.first().cancelExecution()
            }
        }
    }

    private fun onListChanged(list: List<Instruction>) {
        log("onListChanged:", logLevel = Logger.Level.Staging)
        list.forEach { instruction ->
            log(
                "Instruction $instruction. id - ${instruction.id} " +
                    "execution time - ${instruction.executionTime.toDate()} " +
                    "possible delay - ${instruction.possibleDelay} ",
                logLevel = Logger.Level.Staging
            )
        }
        setupNextInstruction()
    }

    private fun setupNextInstruction() {
        if (instructionList.isEmpty().not() && working.not() && started) {
            executeInstruction(instructionList.first())
        }
    }

    private fun executeInstruction(instruction: Instruction) {
        log("executeInstruction id ${instruction.id}", logLevel = Logger.Level.Staging)
        instructionExecutionJob = botScope.launch {
            log(
                "instruction is executing id ${instruction.id} instance of ${instruction.javaClass.simpleName}",
                logLevel = Logger.Level.Staging
            )
            delay(
                getExecutionTime(instruction)
            )
            working = true
            instruction.execute(
                ::onTimeout,
                ::onCanceled,
                ::onExecuted,
                ::onException
            )
        }
    }

    private fun getExecutionTime(instruction: Instruction): Long {
        val currentTime = System.currentTimeMillis()
        val instructionExecutionTime =
            if (instruction.executionTime < instruction.executionTime + instruction.possibleDelay) {
                Random.nextLong(
                    instruction.executionTime,
                    instruction.executionTime + instruction.possibleDelay
                )
            } else {
                instruction.executionTime
            }
        return instructionExecutionTime - currentTime
    }

    private fun onTimeout(instruction: Instruction) {
        log("onTimeout id - ${instruction.id}", logLevel = Logger.Level.Staging)
        onInstructionTimeout(
            InstructionTimeoutException(
                "Instruction timeout after waiting" +
                    " ${instruction.timeout.toDate()}. Instruction description: ${instruction.description}"
            )
        )
        working = false
    }

    private fun onCanceled(instruction: Instruction) {
        log("onCancel id - ${instruction.id}", logLevel = Logger.Level.Staging)
        working = false
        setupNextInstruction()
    }

    private fun onExecuted(instruction: Instruction) {
        log("onExecuted id - ${instruction.id}", logLevel = Logger.Level.Staging)
        instruction.onExecuted(instruction)
        working = false
        instructionList.remove(instruction)
    }

    private fun onException(exception: Exception, instruction: Instruction) {
        log("onException id - ${instruction.id}", logLevel = Logger.Level.Staging)
        log.e(exception)
        onInstructionFailed(exception, instruction)
        instruction.onException(exception, instruction)
        working = false
    }

    private fun Long.toDate(): String {
        return SimpleDateFormat("HH:mm:ss.SSS").format(Date(this))
    }
}
