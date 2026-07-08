package com.aether.renderer

data class SkillExecutionBlockState(
    val executionEnabled: Boolean = false,
    val autoControlEnabled: Boolean = false,
    val tapAllowed: Boolean = false,
    val clickAllowed: Boolean = false,
    val swipeAllowed: Boolean = false,
    val automationAllowed: Boolean = false,
    val reason: String = "AT140 final execution blocker locked"
) {
    val isLocked: Boolean
        get() = !executionEnabled && !autoControlEnabled && !tapAllowed && !clickAllowed && !swipeAllowed && !automationAllowed

    val badge: String
        get() = if (isLocked) "EXECUTION_BLOCKED" else "EXECUTION_UNSAFE"
}

object SkillExecutionBlocker {
    fun locked(): SkillExecutionBlockState = SkillExecutionBlockState()

    fun enforce(descriptor: SkillDescriptor): SkillExecutionBlockState {
        return if (descriptor.executionEnabled || descriptor.canExecute) {
            SkillExecutionBlockState(reason = "descriptor_requested_execution_but_locked")
        } else {
            locked()
        }
    }
}
