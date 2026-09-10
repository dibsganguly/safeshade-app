package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.InMemorySmartHomeStore
import com.safeshade.data.MAX_SMART_HOME_FIRINGS
import com.safeshade.data.SmartHomeHook
import com.safeshade.data.SmartHomeProviders
import com.safeshade.data.SmartHomeTriggers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that decide whether a hook can fire at all: what is stored, what
 * the firing log remembers, and which addresses this app is willing to send a
 * person's emergency to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SmartHomeRepositoryTest {

    private class CountingHooks : SyncHooks {
        val upserts = mutableListOf<Pair<String, String>>()
        val deletes = mutableListOf<Pair<String, String>>()
        override suspend fun onUpsert(table: String, recordId: String) {
            upserts += table to recordId
        }

        override suspend fun onDelete(table: String, recordId: String) {
            deletes += table to recordId
        }
    }

    private fun hook(id: String = "h1", trigger: String = SmartHomeTriggers.FALL) = SmartHomeHook(
        id = id,
        name = "Hall light",
        trigger = trigger,
        provider = SmartHomeProviders.WEBHOOK,
        endpointUrl = "https://example.invalid/hook"
    )

    private fun build(
        store: InMemorySmartHomeStore = InMemorySmartHomeStore(),
        hooks: SyncHooks = SyncHooks.None,
        now: () -> Long = { 1_000L }
    ): Pair<SmartHomeRepository, InMemorySmartHomeStore> {
        val scope = TestScope(UnconfinedTestDispatcher())
        return SmartHomeRepository(store, scope, hooks, now) to store
    }

    // ============================================
    // CRUD and sync
    // ============================================

    @Test
    fun `adding queues the row for the smart_home_hooks table`() = runTest {
        val sync = CountingHooks()
        val (repo, store) = build(hooks = sync)
        repo.add(hook())
        assertEquals(1, store.currentHooks.size)
        assertEquals(listOf(CloudTables.SMART_HOME_HOOKS to "h1"), sync.upserts)
    }

    @Test
    fun `adding the same id twice neither duplicates nor re-queues`() = runTest {
        val sync = CountingHooks()
        val (repo, store) = build(hooks = sync)
        repo.add(hook())
        repo.add(hook())
        assertEquals(1, store.currentHooks.size)
        assertEquals(1, sync.upserts.size)
    }

    @Test
    fun `removing a hook queues a tombstone and takes its firings with it`() = runTest {
        val sync = CountingHooks()
        val (repo, store) = build(hooks = sync)
        repo.add(hook("h1"))
        repo.add(hook("h2"))
        repo.recordFiring("h1", 200, null)
        repo.recordFiring("h2", 200, null)

        repo.remove(hook("h1"))

        assertEquals(listOf("h2"), store.currentHooks.map { it.id })
        assertEquals(listOf("h2"), store.currentFirings.map { it.hookId })
        assertEquals(listOf(CloudTables.SMART_HOME_HOOKS to "h1"), sync.deletes)
    }

    @Test
    fun `a pulled change is not queued back to the server`() = runTest {
        val sync = CountingHooks()
        val (repo, store) = build(hooks = sync)
        repo.applyRemoteHooks { it + hook("remote") }
        assertEquals(listOf("remote"), store.currentHooks.map { it.id })
        assertTrue(sync.upserts.isEmpty())
    }

    // ============================================
    // The firing log
    // ============================================

    @Test
    fun `a delivered firing clears the error and stamps the hook`() = runTest {
        val (repo, store) = build(now = { 5_000L })
        repo.add(hook().copy(lastError = "an old failure", lastStatusCode = 500))
        repo.recordFiring("h1", 204, null)

        val stored = store.currentHooks.single()
        assertEquals(5_000L, stored.lastFiredAt)
        assertEquals(204, stored.lastStatusCode)
        assertNull(stored.lastError)
        assertEquals(1, store.currentFirings.size)
        assertEquals(SmartHomeTriggers.FALL, store.currentFirings.single().trigger)
    }

    @Test
    fun `a failed firing is recorded with its reason, not dropped`() = runTest {
        val (repo, store) = build()
        repo.add(hook())
        repo.recordFiring("h1", null, "Nothing answered at that address.")

        val stored = store.currentHooks.single()
        assertNotNull(stored.lastFiredAt)
        assertNull(stored.lastStatusCode)
        assertEquals("Nothing answered at that address.", stored.lastError)
        assertEquals("Nothing answered at that address.", store.currentFirings.single().error)
    }

    @Test
    fun `the firing log keeps the newest fifty`() = runTest {
        var clock = 0L
        val (repo, store) = build(now = { clock })
        repo.add(hook())
        repeat(70) {
            clock = it.toLong()
            repo.recordFiring("h1", 200, null)
        }
        assertEquals(MAX_SMART_HOME_FIRINGS, store.currentFirings.size)
        assertEquals(69L, store.currentFirings.last().at)
    }

    // ============================================
    // validateUrl
    // ============================================

    private val validator = build().first

    @Test
    fun `https anywhere is accepted`() {
        assertNull(validator.validateUrl("https://example.com/hook"))
        assertNull(validator.validateUrl("https://maker.ifttt.com/trigger/fall/with/key/abc"))
        assertNull(validator.validateUrl("  https://example.com/hook  "))
    }

    @Test
    fun `http on the home network is accepted because Home Assistant lives there`() {
        assertNull(validator.validateUrl("http://192.168.1.20:8123/api/webhook/x"))
        assertNull(validator.validateUrl("http://10.0.0.5:8123/api/webhook/x"))
        assertNull(validator.validateUrl("http://172.16.4.1/x"))
        assertNull(validator.validateUrl("http://172.31.4.1/x"))
        assertNull(validator.validateUrl("http://127.0.0.1:8123/x"))
        assertNull(validator.validateUrl("http://localhost:8123/x"))
        assertNull(validator.validateUrl("http://[::1]:8123/x"))
    }

    @Test
    fun `http to the open internet is refused with a reason`() {
        val reason = validator.validateUrl("http://example.com/hook")
        assertNotNull(reason)
        assertTrue(reason!!.contains("https"))
        assertNotNull(validator.validateUrl("http://172.32.0.1/x"))
        assertNotNull(validator.validateUrl("http://11.0.0.1/x"))
        assertNotNull(validator.validateUrl("http://193.168.1.1/x"))
    }

    @Test
    fun `other schemes and empty input are refused with a reason`() {
        assertNotNull(validator.validateUrl(""))
        assertNotNull(validator.validateUrl("   "))
        assertNotNull(validator.validateUrl("ftp://example.com/x"))
        assertNotNull(validator.validateUrl("example.com/hook"))
        assertNotNull(validator.validateUrl("https://"))
    }

    @Test
    fun `the mDNS Home Assistant name is allowed over http, and only that suffix`() {
        // Decided by the owner on 2026-09-10: a .local name is link-local by
        // definition, so it joins the private-address list. Nothing else does.
        assertNull(validator.validateUrl("http://homeassistant.local:8123/api/webhook/x"))
        assertNotNull(validator.validateUrl("http://homeassistant.lan:8123/api/webhook/x"))
        assertNotNull(validator.validateUrl("http://example.com/hook"))
        // A bare ".local" is not a host.
        assertNotNull(validator.validateUrl("http://.local/hook"))
    }
}
