package com.iamashad.meraki.utils

import android.content.Context
import android.util.Log
import com.iamashad.meraki.model.EmotionCategory
import com.iamashad.meraki.model.EmotionIntensity
import com.iamashad.meraki.model.EmotionResult
import com.iamashad.meraki.utils.EmotionClassifier.Companion.CONFIDENCE_THRESHOLD
import com.iamashad.meraki.utils.EmotionClassifier.Companion.INPUT_SIZE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Phase 3: On-Device Emotion Classifier
 *
 * Performs text emotion classification entirely on-device — zero network calls.
 *
 * ## Inference path (when model is present)
 * Loads `assets/emotion_classifier.tflite`, maps the text into a fixed-size
 * float tensor, runs inference, and reads back 7 probability scores — one per
 * [EmotionCategory] in declaration order (ANXIOUS, SAD, STRESSED, CALM, HAPPY,
 * ANGRY, NEUTRAL).
 *
 * ## Fallback path (no model / low confidence)
 * If the `.tflite` file is absent, or if the model's best score is below
 * [CONFIDENCE_THRESHOLD], the classifier falls back to an enhanced keyword
 * analysis that also estimates [EmotionIntensity] through amplifier/diminisher
 * word detection and keyword density scoring.
 *
 * ### Fallback confidence → NEUTRAL guard
 * A confidence < [CONFIDENCE_THRESHOLD] always returns [EmotionCategory.NEUTRAL]
 * to prevent jittery UI gradient transitions on ambiguous short messages.
 *
 * ## Thread safety
 * [classify] is a `suspend` function that always runs on [Dispatchers.Default].
 * The underlying [Interpreter] is not thread-safe; callers must not invoke
 * [classify] concurrently (the single-request mutex in ChatViewModel guarantees this).
 */
@Singleton
class EmotionClassifier @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "EmotionClassifier"
        private const val MODEL_FILENAME = "emotion_classifier.tflite"

        /**
         * Minimum confidence threshold.
         * Predictions below this value are collapsed to NEUTRAL so the UI does
         * not flash rapid gradient changes when the model is uncertain.
         */
        const val CONFIDENCE_THRESHOLD = 0.40f

        /**
         * Maximum number of words fed into the model's bag-of-words input tensor.
         * Must match the model's input shape: [1, INPUT_SIZE].
         */
        private const val INPUT_SIZE = 128
    }

    // ── TFLite interpreter ────────────────────────────────────────────────────

    /** Lazily-loaded interpreter; null when the model file is not in assets. */
    private val interpreter: Interpreter? by lazy { loadInterpreter() }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadInterpreter(): Interpreter? {
        return try {
            val model: MappedByteBuffer = loadModelFile(context, MODEL_FILENAME)
            val options = Interpreter.Options().apply {
                setNumThreads(2)        // Use 2 threads for inference
                setUseNNAPI(false)      // Avoid NNAPI to guarantee deterministic latency
            }
            Interpreter(model, options).also {
                Log.i(TAG, "TFLite model loaded from assets/$MODEL_FILENAME")
            }
        } catch (e: IOException) {
            // Model not bundled yet — fall back to keyword analysis gracefully.
            Log.w(TAG, "TFLite model not found in assets; using keyword fallback: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite interpreter: ${e.message}", e)
            null
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Classifies the emotional content of [text].
     *
     * Runs on [Dispatchers.Default] so it never blocks the main thread.
     * Returns [EmotionResult] with [EmotionCategory.NEUTRAL] when the
     * confidence is below [CONFIDENCE_THRESHOLD].
     */
    suspend fun classify(text: String): EmotionResult = withContext(Dispatchers.Default) {
        if (text.isBlank()) {
            return@withContext neutralResult(confidence = 0f)
        }

        val result = if (interpreter != null) {
            runCatching { classifyWithModel(text) }.getOrElse {
                Log.w(TAG, "Model inference failed, using fallback: ${it.message}")
                classifyWithFallback(text)
            }
        } else {
            classifyWithFallback(text)
        }

        // Collapse low-confidence predictions to NEUTRAL
        return@withContext if (result.confidence < CONFIDENCE_THRESHOLD) {
            neutralResult(result.confidence)
        } else {
            result
        }
    }

    /** Release native resources. Call when the owning component is destroyed. */
    fun close() {
        interpreter?.close()
    }

    // ── TFLite inference path ─────────────────────────────────────────────────

    /**
     * Runs the text through the TFLite model.
     *
     * Input tensor shape : [1, INPUT_SIZE] of Float (normalized token frequencies)
     * Output tensor shape: [1, 7] of Float (per-category probability scores)
     *
     * Category index order must match [EmotionCategory.entries] declaration order:
     *   0=ANXIOUS, 1=SAD, 2=STRESSED, 3=CALM, 4=HAPPY, 5=ANGRY, 6=NEUTRAL
     */
    private fun classifyWithModel(text: String): EmotionResult {
        val inputBuffer = Array(1) { preprocessText(text) }
        val outputBuffer = Array(1) { FloatArray(EmotionCategory.entries.size) }

        interpreter!!.run(inputBuffer, outputBuffer)

        val scores = outputBuffer[0]
        val bestIdx = scores.indices.maxByOrNull { scores[it] } ?: 6
        val confidence = scores[bestIdx]
        val category = EmotionCategory.entries[bestIdx]

        // Derive intensity from how dominant the winning score is
        val intensity = when {
            confidence > 0.75f -> EmotionIntensity.HIGH
            confidence > 0.55f -> EmotionIntensity.MEDIUM
            else -> EmotionIntensity.LOW
        }

        Log.d(TAG, "TFLite → $category ($intensity) conf=${"%.2f".format(confidence)}")
        return EmotionResult(category, intensity, confidence)
    }

    /**
     * Converts text to a fixed-size float array representing normalized
     * unigram frequencies relative to [emotionVocab].
     *
     * Tokens not present in the vocabulary are ignored. The resulting
     * vector is L2-normalized so magnitude does not bias the model.
     */
    private fun preprocessText(text: String): FloatArray {
        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        val vector = FloatArray(INPUT_SIZE)
        for (token in tokens) {
            val idx = emotionVocab.indexOf(token)
            if (idx in 0 until INPUT_SIZE) {
                vector[idx] += 1f
            }
        }

        // L2 normalization
        val norm = sqrt(vector.map { it * it }.sum().toDouble()).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }

        return vector
    }

    // ── Keyword fallback path ─────────────────────────────────────────────────

    /**
     * Enhanced keyword-based classification that also estimates intensity.
     *
     * Scoring algorithm:
     *  1. Detect amplifiers ("very", "extremely") and diminishers ("a bit", "slightly").
     *  2. Count category keyword hits weighted by amplifier/diminisher multiplier.
     *  3. Normalize scores across all categories to produce a pseudo-confidence.
     *  4. Determine intensity from the raw weighted hit count.
     */
    private fun classifyWithFallback(text: String): EmotionResult {
        val lower = text.lowercase()

        // Amplifier / diminisher detection
        val amplifierMultiplier = when {
            amplifierWords.any { lower.contains(it) } -> 1.6f
            diminisherWords.any { lower.contains(it) } -> 0.6f
            else -> 1.0f
        }

        // Score each category by weighted keyword hits
        val scores = mutableMapOf<EmotionCategory, Float>()
        for (category in EmotionCategory.entries) {
            val categoryWords = fallbackKeywords[category] ?: continue
            val hits = categoryWords.count { kw -> lower.contains(kw) }
            scores[category] = hits * amplifierMultiplier
        }

        val totalScore = scores.values.sum()

        if (totalScore == 0f) {
            return neutralResult(confidence = 0f)
        }

        // Normalize to get pseudo-confidence
        val best = scores.maxByOrNull { it.value }!!
        val confidence = confidence(best.value, totalScore)

        val intensity = intensityFromHits(best.value / amplifierMultiplier, amplifierMultiplier)

        Log.d(
            TAG,
            "Fallback → ${best.key} ($intensity) conf=${"%.2f".format(confidence)} " +
                    "hits=${best.value / amplifierMultiplier} amp=$amplifierMultiplier"
        )
        return EmotionResult(best.key, intensity, confidence)
    }

    private fun confidence(bestScore: Float, totalScore: Float): Float =
        if (totalScore > 0f) (bestScore / totalScore).coerceIn(0f, 1f) else 0f

    private fun intensityFromHits(rawHits: Float, amplifier: Float): EmotionIntensity {
        val effective = rawHits * amplifier
        return when {
            effective >= 3.0f -> EmotionIntensity.HIGH
            effective >= 1.5f -> EmotionIntensity.MEDIUM
            else -> EmotionIntensity.LOW
        }
    }

    private fun neutralResult(confidence: Float) = EmotionResult(
        primary = EmotionCategory.NEUTRAL,
        intensity = EmotionIntensity.LOW,
        confidence = confidence
    )

    // ── Vocabulary / keyword tables ───────────────────────────────────────────

    /**
     * Ordered vocabulary for the TFLite model's bag-of-words input tensor.
     * Index positions must be stable (matching what the model was trained on).
     * Capped at [INPUT_SIZE] entries.
     */
    private val emotionVocab: List<String> = listOf(
        // ANXIOUS
        "anxiety", "anxious", "nervous", "worried", "uneasy", "panic", "panicked",
        "restless", "fear", "scared", "dread", "tense", "on edge", "freaking",
        // SAD
        "sad", "depressed", "depression", "upset", "down", "blue", "heartbroken",
        "lonely", "miserable", "hurt", "crying", "cry", "grief", "loss", "hopeless",
        // STRESSED
        "stress", "stressed", "stressful", "overwhelmed", "pressure", "burnout",
        "exhausted", "overloaded", "deadline", "tired", "drained",
        // CALM
        "calm", "relaxed", "peaceful", "serene", "chill", "tranquil", "content",
        "at ease", "soothing", "centered", "grounded",
        // HAPPY
        "happy", "joyful", "excited", "pleased", "grateful", "blessed", "cheerful",
        "thrilled", "ecstatic", "glad", "elated", "good", "great", "wonderful",
        // ANGRY
        "angry", "mad", "frustrated", "annoyed", "irritated", "furious", "rage",
        "outraged", "resentful", "hostile", "pissed", "livid",
        // NEUTRAL
        "okay", "fine", "neutral", "alright", "meh", "nothing", "normal"
    ).take(INPUT_SIZE)

    /**
     * Per-category keyword lists used in the fallback classifier.
     * Richer than [emotionKeywords] (LocalDataSource) — includes
     * phrases and intensity markers for better signal extraction.
     */
    private val fallbackKeywords: Map<EmotionCategory, List<String>> = mapOf(
        EmotionCategory.ANXIOUS to listOf(
            "anxiety", "anxious", "nervous", "worried", "uneasy", "panic", "panicked",
            "restless", "on edge", "freaking out", "scared", "dread", "tense",
            "what if", "fear", "afraid", "paranoid", "overthinking", "spiraling"
        ),
        EmotionCategory.SAD to listOf(
            "sad", "depressed", "depression", "upset", "down", "blue", "heartbroken",
            "lonely", "miserable", "hurt", "crying", "cry", "grief", "loss",
            "hopeless", "helpless", "empty", "numb", "bummed", "gloomy", "devastated"
        ),
        EmotionCategory.STRESSED to listOf(
            "stress", "stressed", "stressful", "overwhelmed", "pressure", "burnout",
            "exhausted", "overloaded", "deadline", "drained", "so much to do",
            "can't cope", "behind", "falling behind", "too much", "barely managing"
        ),
        EmotionCategory.CALM to listOf(
            "calm", "relaxed", "peaceful", "serene", "chill", "tranquil", "content",
            "at ease", "soothing", "centered", "grounded", "breathe", "okay now",
            "better", "resting", "unwinding", "settled"
        ),
        EmotionCategory.HAPPY to listOf(
            "happy", "joyful", "excited", "pleased", "grateful", "blessed", "cheerful",
            "thrilled", "ecstatic", "glad", "elated", "good", "great", "wonderful",
            "amazing", "love it", "fantastic", "awesome", "pumped", "yay", "overjoyed"
        ),
        EmotionCategory.ANGRY to listOf(
            "angry", "mad", "frustrated", "annoyed", "irritated", "furious", "rage",
            "outraged", "resentful", "hostile", "pissed", "livid", "fed up",
            "sick of", "hate", "can't stand", "infuriated", "boiling"
        ),
        EmotionCategory.NEUTRAL to listOf(
            "okay", "fine", "neutral", "alright", "meh", "nothing", "normal",
            "whatever", "not sure", "so so", "average", "just there", "indifferent"
        )
    )

    /**
     * Words that amplify the emotional intensity of a statement.
     * When any of these appear in the input, keyword scores are multiplied by 1.6.
     */
    private val amplifierWords: List<String> = listOf(
        "very", "extremely", "really", "so", "incredibly", "absolutely",
        "terribly", "utterly", "deeply", "intensely", "overwhelmingly", "insanely"
    )

    /**
     * Words that diminish the emotional intensity of a statement.
     * When any of these appear in the input, keyword scores are multiplied by 0.6.
     */
    private val diminisherWords: List<String> = listOf(
        "a bit", "slightly", "somewhat", "a little", "kind of", "sort of",
        "barely", "mildly", "not very", "not that", "a tad", "rather"
    )
}
