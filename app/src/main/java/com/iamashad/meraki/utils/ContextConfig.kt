package com.iamashad.meraki.utils

/**
 * Token-budget constants for the dynamic context window (Phase 1).
 *
 * Budget breakdown (all values in approximate tokens; total = MAX_CONTEXT_TOKENS):
 *
 *   ┌─────────────────────────┬────────┐
 *   │ SYSTEM_PROMPT_RESERVE   │   400  │  Space for getSystemInstructions()
 *   │ USER_PROFILE_RESERVE    │   300  │  Reserved for Phase 4 personalisation data
 *   │ RESPONSE_BUFFER         │   300  │  Headroom for the model's reply
 *   │ AVAILABLE_FOR_HISTORY   │  1000  │  What remains for conversation turns
 *   ├─────────────────────────┼────────┤
 *   │ MAX_CONTEXT_TOKENS      │  2000  │  Soft cap on tokens sent per API call
 *   └─────────────────────────┴────────┘
 *
 * These are intentionally conservative estimates — the real Gemini context window
 * is much larger, but capping here keeps latency and cost predictable while the
 * token-estimation heuristic (length / 4) is still a rough approximation.
 */
object ContextConfig {
    const val MAX_CONTEXT_TOKENS    = 2000
    const val SYSTEM_PROMPT_RESERVE = 400
    const val USER_PROFILE_RESERVE  = 300   // Reserved for Phase 4 user-profile injection
    const val RESPONSE_BUFFER       = 300
    const val AVAILABLE_FOR_HISTORY = MAX_CONTEXT_TOKENS -
            SYSTEM_PROMPT_RESERVE -
            USER_PROFILE_RESERVE -
            RESPONSE_BUFFER          // = 1000
}
