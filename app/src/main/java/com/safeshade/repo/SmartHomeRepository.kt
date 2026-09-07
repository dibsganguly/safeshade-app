package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.MAX_SMART_HOME_FIRINGS
import com.safeshade.data.SmartHomeFiring
import com.safeshade.data.SmartHomeHook
import com.safeshade.data.SmartHomeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI

/**
 * The smart-home hooks, and the log of what each one actually did.
 *
 * ### Why the firing log is not optional
 *
 * A hook is a promise that the hall light comes on when someone falls. The
 * failure mode is silence: an endpoint that moved, a token that expired, a
 * router that stopped forwarding - none of which the phone finds out about
 * unless it writes down what happened. So every attempt is recorded through
 * [recordFiring] with the real status and the real reason, delivered or not,
 * and the hook itself carries the last one so a list row can say what it is
 * currently doing without anybody opening the log.
 *
 * ### Nothing here fires anything
 *
 * Deciding *when* to fire is `service/SmartHomeRunner`, and doing the posting
 * is `platform/SmartHomeApps.post`. This class holds rows and rules and has no
 * network in it, which is what lets the rules be tested against
 * `InMemorySmartHomeStore` with no Android under them.
 */
class SmartHomeRepository(
    private val store: SmartHomeStore,
    private val scope: CoroutineScope,
    /** See [SyncHooks]. Nothing is queued for the cloud without one. */
    private val syncHooks: SyncHooks = SyncHooks.None,
    /** Overridable so tests can control the firing timestamps. */
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Null until the first store read completes.
     *
     * The distinction matters: null is "we have not looked yet" and an empty
     * list is "there are none". A screen drawing "No hooks yet" for the first
     * would be stating a fact it does not have.
     */
    val hooks: StateFlow<List<SmartHomeHook>?> =
        store.hooks.stateIn(scope, SharingStarted.Eagerly, null)

    val firings: StateFlow<List<SmartHomeFiring>?> =
        store.firings.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Serialises every read-modify-write of the hook list and the firing ring.
     *
     * One lock for both, because [recordFiring] touches both in one operation
     * and two locks taken in one place is two locks that can be taken in the
     * other order somewhere else.
     */
    private val lock = Mutex()

    // ============================================
    // CRUD
    // ============================================

    suspend fun add(hook: SmartHomeHook) {
        val added = lock.withLock {
            val current = store.hooks.first()
            if (current.any { it.id == hook.id }) return@withLock false
            store.setHooks(current + hook)
            true
        }
        if (added) syncHooks.onUpsert(CloudTables.SMART_HOME_HOOKS, hook.id)
    }

    suspend fun update(hook: SmartHomeHook) {
        lock.withLock {
            val current = store.hooks.first()
            store.setHooks(current.map { if (it.id == hook.id) hook else it })
        }
        syncHooks.onUpsert(CloudTables.SMART_HOME_HOOKS, hook.id)
    }

    suspend fun remove(hook: SmartHomeHook) = removeById(hook.id)

    suspend fun removeById(hookId: String) {
        lock.withLock {
            val current = store.hooks.first()
            store.setHooks(current.filterNot { it.id == hookId })
            // The firings go with it. A firing row names a hook, and a row
            // naming a hook that no longer exists is a line in a log the UI
            // cannot label.
            val currentFirings = store.firings.first()
            val kept = currentFirings.filterNot { it.hookId == hookId }
            if (kept.size != currentFirings.size) store.setFirings(kept)
        }
        syncHooks.onDelete(CloudTables.SMART_HOME_HOOKS, hookId)
    }

    /**
     * Hooks arriving from another phone in the circle.
     *
     * Under the same [lock] as every local write, and deliberately **not**
     * calling [syncHooks]: a pulled row that queued its own push would be echoed
     * back to the server, whose `updated_at` trigger would make it look like a
     * change, which would pull it again. That loop has no exit.
     */
    suspend fun applyRemoteHooks(transform: (List<SmartHomeHook>) -> List<SmartHomeHook>) {
        lock.withLock {
            val current = store.hooks.first()
            val updated = transform(current)
            if (updated != current) store.setHooks(updated)
        }
    }

    // ============================================
    // Firing log
    // ============================================

    /**
     * Writes down one attempt, and stamps the hook with it.
     *
     * @param statusCode the HTTP status, or null when nothing was reached at
     *   all. Null is not a failure code; [error] is what says it failed.
     * @param error plain English from whatever refused, or null when the call
     *   was delivered. Never a URL and never a secret - see
     *   `SmartHomeStore`'s KDoc on why the endpoint counts as one.
     *
     * `lastFiredAt` is set on every attempt, not only on the successful ones.
     * It answers "when did this last try", which is the question a user
     * debugging a silent hook is actually asking; whether it worked is
     * [SmartHomeHook.lastError]'s job.
     */
    suspend fun recordFiring(hookId: String, statusCode: Int?, error: String?) {
        val at = now()
        lock.withLock {
            val current = store.hooks.first()
            val hook = current.firstOrNull { it.id == hookId }
            if (hook != null) {
                store.setHooks(
                    current.map {
                        if (it.id == hookId) {
                            it.copy(
                                lastFiredAt = at,
                                lastError = error,
                                lastStatusCode = statusCode
                            )
                        } else {
                            it
                        }
                    }
                )
            }
            val firing = SmartHomeFiring(
                hookId = hookId,
                at = at,
                trigger = hook?.trigger ?: "",
                statusCode = statusCode,
                error = error
            )
            store.setFirings((store.firings.first() + firing).takeLast(MAX_SMART_HOME_FIRINGS))
        }
        // The last-fired stamp is worth syncing - it is how a second guardian's
        // phone sees that the hook is alive - but only when the hook exists.
        if (store.hooks.first().any { it.id == hookId }) {
            syncHooks.onUpsert(CloudTables.SMART_HOME_HOOKS, hookId)
        }
    }

    /** The firing log for one hook, newest last. */
    fun firingsFor(hookId: String): List<SmartHomeFiring> =
        firings.value.orEmpty().filter { it.hookId == hookId }

    // ============================================
    // Validation
    // ============================================

    /**
     * Whether [url] is somewhere this app is willing to send an alert, and why
     * not when it is not.
     *
     * ### The rule
     *
     * `https` anywhere. `http` **only** to an address on the local network -
     * `localhost`, `127.x`, `::1`, `10.x`, `172.16-31.x` and `192.168.x`.
     *
     * That exception exists because Home Assistant on a home LAN is the common
     * case, and the common Home Assistant address is a plain `http://` one on
     * `192.168.x.x:8123`. Refusing it would mean refusing the single provider
     * this feature exists to serve. Over the open internet the same call would
     * put a person's name, position and the fact that they have just fallen
     * into cleartext on somebody else's network, so `http` stops at the edge of
     * the house.
     *
     * ### Known gap
     *
     * `http://homeassistant.local:8123` - the mDNS name Home Assistant's own
     * onboarding prints - is **rejected** by this rule, because a hostname
     * cannot be checked for being local without resolving it, and resolving a
     * name typed into a settings field is a network call in a validator. Users
     * on that address should enter the numeric one. Whether `.local` should
     * join the list is a product decision, not one this function should make
     * quietly.
     *
     * @return null when the URL is fine, or one plain sentence saying what is
     *   wrong with it.
     */
    fun validateUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return "Enter the address the alert should be sent to."

        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return "That does not look like a web address."

        val scheme = uri.scheme?.lowercase()
            ?: return "Start the address with https:// so it is sent securely."
        val host = uri.host?.lowercase()
            ?: return "That address is missing a host name, so there is nowhere to send to."
        if (host.isBlank()) {
            return "That address is missing a host name, so there is nowhere to send to."
        }

        return when (scheme) {
            "https" -> null
            "http" ->
                if (isPrivateHost(host)) {
                    null
                } else {
                    "Plain http is only allowed to an address on your own network. " +
                        "Use https:// for anything on the internet."
                }

            else -> "Only https:// addresses are accepted, or http:// on your own network."
        }
    }

    /**
     * Whether [host] names something on the local network.
     *
     * Literal addresses only. A hostname is never assumed local - see the gap
     * noted on [validateUrl].
     */
    private fun isPrivateHost(host: String): Boolean {
        // IPv6 comes out of URI.getHost() wrapped in brackets.
        val bare = host.removePrefix("[").removeSuffix("]")
        if (bare == "localhost" || bare == "::1" || bare == "0:0:0:0:0:0:0:1") return true

        val parts = bare.split(".")
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false

        return when {
            octets[0] == 127 -> true
            octets[0] == 10 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            // Link-local, which is what a phone talking to a device over a
            // direct Wi-Fi link ends up on.
            octets[0] == 169 && octets[1] == 254 -> true
            else -> false
        }
    }
}
