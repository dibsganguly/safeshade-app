package com.safeshade.cloud

import android.content.Context
import com.safeshade.BuildConfig
import com.safeshade.cloud.sync.Connectivity
import com.safeshade.cloud.sync.NoPayloadSource
import com.safeshade.cloud.sync.NoPullSource
import com.safeshade.cloud.sync.Outbox
import com.safeshade.cloud.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The cloud half of the object graph: client, auth, outbox, sync engine.
 *
 * Built and owned by `AppContainer`, on the same application-lifetime scope.
 * Not a `viewModelScope` — a queued fall alert has to reach the server whether
 * or not anybody has the app open, which is precisely when it matters.
 *
 * ### Configured or not is decided here, once
 *
 * `BuildConfig.SUPABASE_URL` is blank on any checkout that has not been through
 * `supabase/README.md`, which includes every fresh clone. Rather than sprinkle
 * `if (cloudEnabled)` through the app, that one check happens here and picks a
 * different [CloudClient]: [FakeCloudClient] with `disabled = true`, whose every
 * method returns [CloudResult.Disabled].
 *
 * The consequence is worth stating plainly, because it is the whole design:
 * **no call site anywhere else needs to know whether cloud exists.** A screen
 * calls `upsert`, gets `Disabled` back, and renders nothing — not an error, not
 * a spinner, not a "cloud unavailable" banner at a user who never asked for
 * cloud. The app is offline-first and stays fully usable; the cloud is an
 * addition to it, never a dependency of it.
 *
 * ### Nothing here does I/O in a constructor
 *
 * The outbox is read from disk in a `launch`, and the sync engine's triggers
 * are wired in the same place. `AppContainer` is constructed on the main thread
 * during `Application.onCreate`, and a DataStore read there is a frame the user
 * spends looking at a white screen.
 */
class CloudContainer(
    appContext: Context,
    private val scope: CoroutineScope
) {

    private val context: Context = appContext.applicationContext

    /** True when this build has a project to talk to. */
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    /**
     * The client.
     *
     * Note both halves of the check: a URL with no key produces a client that
     * builds fine and then 401s on every single call, which is a far more
     * confusing failure than being cleanly disabled.
     */
    val client: CloudClient = if (isConfigured) {
        SupabaseCloudClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            scope = scope
        )
    } else {
        FakeCloudClient(disabled = true)
    }

    val auth = CloudAuth(client)

    val outbox = Outbox(context)

    private val connectivity = Connectivity(context)

    /**
     * The sync engine, with Phase 1's no-op sources installed.
     *
     * [NoPayloadSource] resolves no row bodies, so **nothing is actually
     * uploaded yet** — the queue fills, the triggers fire, the drain runs and
     * finds nothing it can send. That is deliberate: the payload rules belong
     * with the repositories that own the data, and inventing them here would
     * put a second, competing definition of each record's wire shape in the
     * one place least likely to be updated when the record changes.
     *
     * Crucially, a null payload is a *skip*, not a failure — see
     * [com.safeshade.cloud.sync.PayloadSource.payloadFor]. Without that rule
     * every queued record would march to five attempts and report itself as
     * failed, and the app would be telling users their data did not reach a
     * cloud it never tried to send to.
     */
    val syncEngine = SyncEngine(
        client = client,
        outbox = outbox,
        connectivity = connectivity,
        scope = scope,
        payloadSource = NoPayloadSource,
        pullSource = NoPullSource
    )

    init {
        scope.launch {
            outbox.load()
            // Only when there is somewhere to sync to. Registering a network
            // callback and a lifecycle observer on a build with no project
            // would cost battery to accomplish nothing.
            if (isConfigured) syncEngine.start()
        }
    }
}
