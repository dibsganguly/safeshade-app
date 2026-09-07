package com.safeshade.cloud

import android.content.Context
import com.safeshade.BuildConfig
import com.safeshade.cloud.repo.CloudSyncHooks
import com.safeshade.cloud.repo.RepositoryPayloadSource
import com.safeshade.cloud.repo.RepositoryPullSource
import com.safeshade.cloud.repo.SyncBackfill
import com.safeshade.cloud.sync.Connectivity
import com.safeshade.cloud.sync.NoPayloadSource
import com.safeshade.cloud.sync.NoPullSource
import com.safeshade.cloud.sync.Outbox
import com.safeshade.cloud.sync.PayloadSource
import com.safeshade.cloud.sync.PullSource
import com.safeshade.cloud.sync.SyncEngine
import com.safeshade.cloud.sync.VoiceCloud
import com.safeshade.repo.MessagingRepository
import com.safeshade.repo.ProfileRepository
import com.safeshade.repo.SafetyRepository
import com.safeshade.repo.SyncHooks
import com.safeshade.repo.VoiceNoteRepository
import com.safeshade.repo.ZoneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

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
    private val scope: CoroutineScope,
    /**
     * The repositories the push reads from and the pull writes back into.
     *
     * Nullable because `CloudContainer` is constructed in one place that has no
     * repositories at all - a preview or a test that only wants the account
     * surface. With them absent the engine keeps its no-op sources: the queue
     * still fills, the triggers still fire, and nothing is sent, which is
     * exactly the Phase 1 behaviour.
     */
    private val repositories: Repositories? = null
) {

    /**
     * What the sync engine needs from the rest of the app.
     *
     * Grouped into one parameter rather than four so that adding a fifth
     * repository to the sync does not change `AppContainer`'s call, and so that
     * "the cloud reads these and writes these" is one thing to read.
     */
    class Repositories(
        val profiles: ProfileRepository,
        val safety: SafetyRepository,
        val messaging: MessagingRepository,
        val zones: ZoneRepository,
        /**
         * The Talk thread's voice notes.
         *
         * **Defaulted to null, and null is not the intended wiring.** A voice
         * note queues itself against `messages` when it is recorded
         * (`VoiceNoteRepository.add`), so with this absent the drain finds no
         * row body for that id, counts three skips and reports the note as
         * "There was nothing left on this phone to send for this" - about a
         * recording that never left the phone because nothing here could reach
         * it. `AppContainer` passes `voice = voiceNoteRepository`; the default
         * exists only for the graphs that have no repositories at all.
         */
        val voice: VoiceNoteRepository? = null
    )

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
    /**
     * The Circle: which one this account owns, who is in it, what has been
     * invited, and what tier is paid for.
     *
     * Constructed before the engine because the payload source has to be able
     * to ask it for the circle id, and every circle-scoped row is unsendable
     * without one.
     */
    val circle = CircleManager(
        appContext = context,
        client = client,
        outbox = outbox,
        scope = scope
    )

    /** What the Circle and Plan screens read. See [CloudState]. */
    val cloudState: StateFlow<CloudState> get() = circle.state

    /**
     * Invite, accept an invitation, set the developer tier, read the heat map,
     * and turn the sharing of an alert's place on or off.
     */
    val circleActions: CircleManager.CircleActions get() = circle.actions

    /**
     * The bytes behind a voice note, both ways.
     *
     * Built here rather than in `AppContainer` because it needs the same
     * `filesDir/voice` directory the repository was given and nothing else -
     * and because the upload has to happen inside the push, which is this
     * container's business. The directory is named in exactly two places and
     * this is the second; a mismatch would show up as every upload reporting
     * "That recording is no longer on this phone."
     */
    val voiceCloud: VoiceCloud = VoiceCloud(
        client = client,
        notes = repositories?.voice,
        voiceDir = File(context.filesDir, VOICE_DIR)
    )

    private val pullSource: PullSource = repositories?.let { repos ->
        RepositoryPullSource(
            profiles = repos.profiles,
            safety = repos.safety,
            messaging = repos.messaging,
            zones = repos.zones,
            voice = repos.voice,
            outbox = outbox,
            session = client.session,
            circleIdProvider = { circle.cachedCircleId() },
            sideTables = { table, rows -> circle.onSideRows(table, rows) }
        )
    } ?: NoPullSource

    private val repositoryPayloads: RepositoryPayloadSource? = repositories?.let { repos ->
        RepositoryPayloadSource(
            profiles = repos.profiles,
            safety = repos.safety,
            messaging = repos.messaging,
            zones = repos.zones,
            session = client.session,
            circleId = { circle.cachedCircleId() },
            voice = repos.voice,
            voiceCloud = voiceCloud,
            // Read at resolve time rather than captured, so turning the switch
            // off takes effect on the very next alert this phone pushes.
            shareAlertPlaces = { circle.state.value.shareAlertPlaces }
        )
    }

    private val payloadSource: PayloadSource = repositoryPayloads ?: NoPayloadSource

    val syncEngine = SyncEngine(
        client = client,
        outbox = outbox,
        connectivity = connectivity,
        scope = scope,
        payloadSource = payloadSource,
        pullSource = pullSource
    )

    /**
     * What the repositories call when they write.
     *
     * `AppContainer` binds this into the `LateBoundSyncHooks` it handed the
     * repositories, after this container exists. The alternative - constructing
     * the cloud before the repositories - would put `CloudContainer` anywhere
     * but last in `AppContainer`, and that position is the enforcement of cloud
     * being strictly additive (handoff7 section 2).
     */
    val syncHooks: SyncHooks = CloudSyncHooks(
        outbox = outbox,
        syncEngine = syncEngine,
        session = { client.session.value }
    )

    private companion object {
        /**
         * `filesDir/voice`, matching what `AppContainer` hands
         * `VoiceNoteRepository`. One name, spelled the same in both places.
         */
        const val VOICE_DIR = "voice"
    }

    init {
        // A row arriving on the Realtime socket takes the same path as a row
        // arriving in a pull. Two paths would mean two merge rules.
        circle.onRealtimeRow = { table, rows -> pullSource.onRowsPulled(table, rows) }

        // The records that existed before anybody signed in. Routed through the
        // same hooks a local write uses, so there is one definition of "queue
        // this record" and not two.
        repositoryPayloads?.let { payloads ->
            val backfill = SyncBackfill(payloads, syncHooks)
            circle.onBackfill = { backfill.enqueueAll() }
        }

        scope.launch {
            outbox.load()
            // Only when there is somewhere to sync to. Registering a network
            // callback and a lifecycle observer on a build with no project
            // would cost battery to accomplish nothing.
            if (isConfigured) {
                syncEngine.start()
                circle.start(syncEngine)
            }
        }
    }
}
