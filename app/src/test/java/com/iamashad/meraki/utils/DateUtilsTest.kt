package com.iamashad.meraki.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Unit tests for the "HH:mm" date-formatting pattern used in JournalCard
 * via [android.text.format.DateFormat.format].
 *
 * [android.text.format.DateFormat] is an Android-SDK class and is not
 * available on the JVM test host.  These tests use an equivalent
 * [SimpleDateFormat] with the same "HH:mm" pattern to validate the
 * formatting contract independently of the Android runtime.
 *
 * All assertions are pinned to UTC so results are machine-independent.
 */
class DateUtilsTest {

    /** Helper that creates a UTC-pinned formatter for the JournalCard pattern. */
    private fun utcFormatter(pattern: String = "HH:mm") =
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    // ── output shape ─────────────────────────────────────────────────────────

    @Test
    fun `format produces two-digit hour and two-digit minute separated by colon`() {
        val result = utcFormatter().format(Date(0L)) // 1970-01-01 00:00 UTC
        assertThat(result).matches("\\d{2}:\\d{2}")
    }

    @Test
    fun `format always matches HH_colon_mm regex for any current timestamp`() {
        val result = utcFormatter().format(Date(System.currentTimeMillis()))
        assertThat(result).matches("\\d{2}:\\d{2}")
    }

    // ── known values ─────────────────────────────────────────────────────────

    @Test
    fun `format returns 00_00 for epoch zero (midnight UTC)`() {
        assertThat(utcFormatter().format(Date(0L))).isEqualTo("00:00")
    }

    @Test
    fun `format pads single-digit hour to two digits`() {
        // 1970-01-01 01:05 UTC → "01:05"
        val oneHourFiveMin = (1 * 60 + 5) * 60 * 1000L
        assertThat(utcFormatter().format(Date(oneHourFiveMin))).isEqualTo("01:05")
    }

    @Test
    fun `format returns 12_00 for noon UTC`() {
        val noonUtc = 12L * 3600 * 1000
        assertThat(utcFormatter().format(Date(noonUtc))).isEqualTo("12:00")
    }

    @Test
    fun `format returns 23_59 for one minute before midnight UTC`() {
        val beforeMidnight = (23 * 3600 + 59 * 60) * 1000L
        assertThat(utcFormatter().format(Date(beforeMidnight))).isEqualTo("23:59")
    }

    // ── consistency ──────────────────────────────────────────────────────────

    @Test
    fun `format is idempotent - same timestamp always yields same string`() {
        val sdf = utcFormatter()
        val ts = 9 * 3600 * 1000L // 09:00 UTC
        val first = sdf.format(Date(ts))
        val second = sdf.format(Date(ts))
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `format is consistent across two independent formatter instances`() {
        val ts = 15L * 3600 * 1000 // 15:00 UTC
        val result1 = utcFormatter().format(Date(ts))
        val result2 = utcFormatter().format(Date(ts))
        assertThat(result1).isEqualTo(result2)
    }

    // ── length contract ───────────────────────────────────────────────────────

    @Test
    fun `format output is always exactly 5 characters`() {
        val sdf = utcFormatter()
        listOf(0L, 3600_000L, 43200_000L, 82800_000L).forEach { ts ->
            assertThat(sdf.format(Date(ts))).hasLength(5)
        }
    }

    // ── boundary values ───────────────────────────────────────────────────────

    @Test
    fun `format handles large future timestamps correctly`() {
        // 2030-01-01 06:30 UTC (approx)
        val futureTs = 1893466200_000L
        val result = utcFormatter().format(Date(futureTs))
        assertThat(result).matches("\\d{2}:\\d{2}")
    }
}
