package com.iamashad.meraki.model

/**
 * Phase 3: On-Device Emotion Intelligence
 *
 * Canonical set of emotion categories recognised by the on-device classifier.
 * Maps 1-to-1 to the output nodes of the TFLite model.
 *
 * [key] is the lowercase string used as a gradientMap lookup key and persisted
 * as the "emotion" column in the emotion_logs table, maintaining backward
 * compatibility with the existing string-keyed gradient palette.
 */
enum class EmotionCategory(val displayName: String) {
    ANXIOUS("Anxious"),
    SAD("Sad"),
    STRESSED("Stressed"),
    CALM("Calm"),
    HAPPY("Happy"),
    ANGRY("Angry"),
    NEUTRAL("Neutral");

    /** Lowercase key — matches gradientMap keys and the old analyzeEmotion() return values. */
    val key: String get() = name.lowercase()
}

/**
 * Perceived intensity of the detected emotion.
 * Derived from keyword density + modifier strength in the fallback path,
 * or from logit magnitude in the TFLite path.
 */
enum class EmotionIntensity {
    LOW,
    MEDIUM,
    HIGH;

    val displayName: String get() = name.lowercase()
}

/**
 * Result produced by [EmotionClassifier.classify].
 *
 * @param primary    The dominant emotion category.
 * @param intensity  The perceived strength of that emotion.
 * @param confidence Model confidence in [0, 1]; values below [EmotionClassifier.CONFIDENCE_THRESHOLD]
 *                   always yield [EmotionCategory.NEUTRAL] as the primary emotion.
 */
data class EmotionResult(
    val primary: EmotionCategory,
    val intensity: EmotionIntensity,
    val confidence: Float
)
