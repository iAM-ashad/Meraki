package com.iamashad.meraki.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit [TestWatcher] rule that swaps [Dispatchers.Main] with a
 * [TestDispatcher] for the duration of each test, then restores it.
 *
 * This is essential for any ViewModel under test because [viewModelScope]
 * is bound to [Dispatchers.Main]. Without this rule the scope launch would
 * fail in a JVM unit-test environment where no Android Main dispatcher exists.
 *
 * Usage:
 * ```kotlin
 * @get:Rule val mainDispatcherRule = MainDispatcherRule()
 * ```
 *
 * Defaults to [UnconfinedTestDispatcher] so that coroutines launched on
 * [Dispatchers.Main] run eagerly (without needing manual advancement).
 * Pass a [TestDispatcher] explicitly when you need to control virtual time
 * (e.g. to test [kotlinx.coroutines.flow.debounce]).
 *
 * The exposed [testDispatcher] lets callers share the same
 * [kotlinx.coroutines.test.TestCoroutineScheduler] with [runTest]:
 * ```kotlin
 * @Test
 * fun myTest() = runTest(mainDispatcherRule.testDispatcher) { ... }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
