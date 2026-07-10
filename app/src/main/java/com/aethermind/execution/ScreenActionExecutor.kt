package com.aethermind.execution

interface ScreenActionExecutor {
    suspend fun execute(command: ActionCommand)
}
