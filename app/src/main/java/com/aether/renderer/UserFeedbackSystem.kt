package com.aether.renderer

enum class UserFeedbackKind {
    NONE,
    HELPFUL,
    CONFUSING,
    TOO_NOISY,
    NEEDS_CAUTION
}

data class UserFeedbackState(
    val lastKind: UserFeedbackKind = UserFeedbackKind.NONE,
    val feedbackCount: Int = 0,
    val helpfulCount: Int = 0,
    val cautionCount: Int = 0,
    val feedbackBadge: String = "FEEDBACK_READY",
    val feedbackReason: String = "session_only_feedback"
) {
    val compactLine: String
        get() = "$feedbackBadge last=${lastKind.name} n=$feedbackCount helpful=$helpfulCount caution=$cautionCount"
}

object UserFeedbackSystem {
    private var state = UserFeedbackState()

    fun submit(kind: UserFeedbackKind): UserFeedbackState {
        val helpful = state.helpfulCount + if (kind == UserFeedbackKind.HELPFUL) 1 else 0
        val caution = state.cautionCount + if (kind == UserFeedbackKind.NEEDS_CAUTION || kind == UserFeedbackKind.CONFUSING) 1 else 0
        val count = state.feedbackCount + 1
        state = UserFeedbackState(
            lastKind = kind,
            feedbackCount = count,
            helpfulCount = helpful,
            cautionCount = caution,
            feedbackBadge = when (kind) {
                UserFeedbackKind.HELPFUL -> "FEEDBACK_HELPFUL"
                UserFeedbackKind.CONFUSING -> "FEEDBACK_CONFUSING"
                UserFeedbackKind.TOO_NOISY -> "FEEDBACK_TOO_NOISY"
                UserFeedbackKind.NEEDS_CAUTION -> "FEEDBACK_CAUTION"
                UserFeedbackKind.NONE -> "FEEDBACK_READY"
            },
            feedbackReason = "session_only_no_raw_text"
        )
        return state
    }

    fun reset(): UserFeedbackState {
        state = UserFeedbackState(feedbackBadge = "FEEDBACK_RESET", feedbackReason = "manual_feedback_reset")
        return state
    }

    fun current(): UserFeedbackState = state
}
