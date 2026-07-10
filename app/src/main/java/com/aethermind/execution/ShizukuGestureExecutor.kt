package com.aethermind.execution

class ShizukuGestureExecutor(
    private val coordinateSpace: CoordinateSpace = CoordinateSpace.PIXELS,
    private val commandRunner: suspend (String) -> Unit
) : ScreenActionExecutor {
    override suspend fun execute(command: ActionCommand) {
        when (command.type) {
            ActionCommandType.TAP -> {
                commandRunner("input tap ${command.x.toInt()} ${command.y.toInt()}")
            }

            ActionCommandType.SWIPE -> {
                val endX = command.x.toInt()
                val endY = (command.y - 500f).toInt()
                commandRunner(
                    "input swipe ${command.x.toInt()} ${command.y.toInt()} $endX $endY 220"
                )
            }
        }
    }
}
