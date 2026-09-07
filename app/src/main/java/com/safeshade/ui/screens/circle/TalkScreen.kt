package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.data.VoiceUpload
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.Waveform
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** One voice note as the page draws it. */
data class TalkNote(
    val id: String,
    val fromGuardian: Boolean,
    val authorName: String,
    val timeLabel: String,
    val durationMs: Int,
    val waveform: List<Float>,
    val uploadState: VoiceUpload,
    val listened: Boolean,
    /** Whether the audio is on this phone. False for a note recorded elsewhere and not fetched yet. */
    val onThisPhone: Boolean
)

/** Everything the Talk page draws. */
data class TalkUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val guardianName: String = "",
    /** Oldest first. */
    val notes: List<TalkNote> = emptyList(),
    val recording: Boolean = false,
    val recordingMs: Int = 0,
    val maxMs: Int = 20_000,
    /** The note playing now, and how far through it is. */
    val playingId: String? = null,
    val progress: Float = 0f,
    val signedIn: Boolean = false,
    /** The last thing that went wrong, verbatim. */
    val error: String? = null
)

/**
 * Talk: push-to-talk voice notes between the Circle's phones.
 *
 * A walkie-talkie, not a voicemail: one control, held to speak and released
 * to send, up to twenty seconds. Each note is a plate with its author, its
 * time, its own waveform, a play control and a state word the note can
 * stand behind - On this phone, Sending, On SafeShade Cloud, or Not sent
 * with the reason. No tick is drawn before the upload has completed,
 * because until then nobody else can hear it.
 *
 * The microphone permission is asked for at the first hold, and a refusal
 * is reported as the reason the control does nothing, not swallowed.
 */
@Composable
fun TalkScreen(
    state: TalkUiState,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onPlay: (id: String) -> Unit,
    onStop: () -> Unit,
    onOpenSignIn: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val listState = rememberLazyListState()
    val other = counterpartName(state.role, state.wearerName, state.guardianName)

    LaunchedEffect(state.notes.size) {
        if (state.notes.isNotEmpty()) listState.animateScrollToItem(state.notes.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .padding(
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding()
            )
    ) {
        ScreenHeader(
            title = "Talk",
            subtitle = "Voice notes with $other, phone to phone",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = Spacing.gutter)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (state.notes.isEmpty()) {
                item("empty") {
                    Text(
                        text = "Nothing said yet. Hold the button, speak, let go.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted
                    )
                }
            }
            items(state.notes, key = { it.id }) { note ->
                NotePlate(
                    note = note,
                    role = state.role,
                    wearerName = state.wearerName,
                    guardianName = state.guardianName,
                    playing = state.playingId == note.id,
                    progress = if (state.playingId == note.id) state.progress else 0f,
                    signedIn = state.signedIn,
                    onPlay = { onPlay(note.id) },
                    onStop = onStop
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md)) {
            if (!state.signedIn) {
                Text(
                    text = "Notes stay on this phone until you sign in. Sign in and they reach the rest of the Circle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkTrip,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            HoldToTalk(
                recording = state.recording,
                elapsedMs = state.recordingMs,
                maxMs = state.maxMs,
                onHoldStart = onHoldStart,
                onHoldEnd = onHoldEnd
            )
        }
    }
}

/**
 * The one control: a plate that records while it is held.
 *
 * A press-and-hold rather than a tap-to-start, tap-to-stop pair, because a
 * note that is still recording when the person thinks it has stopped is
 * the failure that matters here, and a finger lifting is unambiguous. The
 * plate says which state it is in, in words, and the spoken description
 * says the same.
 */
@Composable
private fun HoldToTalk(
    recording: Boolean,
    elapsedMs: Int,
    maxMs: Int,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit
) {
    val colors = MaterialTheme.board
    val shape = RoundedCornerShape(16.dp)
    val edge = if (recording) colors.lampTrip else colors.brass
    val word = if (recording) "Recording · ${clockMs(elapsedMs)} of ${clockMs(maxMs)} · let go to send" else "Hold to talk"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(colors.plate)
            .border(2.dp, edge, shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHoldStart()
                        tryAwaitRelease()
                        onHoldEnd()
                    }
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = word
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PilotLamp(state = if (recording) LampState.TRIP else LampState.OFF)
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = word,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
        }
    }
}

@Composable
private fun NotePlate(
    note: TalkNote,
    role: UserRole,
    wearerName: String,
    guardianName: String,
    playing: Boolean,
    progress: Float,
    signedIn: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    val colors = MaterialTheme.board
    val mine = (role == UserRole.GUARDIAN) == note.fromGuardian
    val author = when {
        mine -> "You"
        note.authorName.isNotBlank() -> note.authorName
        note.fromGuardian -> guardianName.ifBlank { "A guardian" }
        else -> wearerName.ifBlank { "The wearer" }
    }
    val (lamp, word, line) = noteState(note, signedIn, mine)
    val spoken = "$author, ${note.timeLabel}, ${clockMs(note.durationMs)}, $word. $line"

    BoardPlate(modifier = Modifier.fillMaxWidth().semantics { contentDescription = spoken }) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = note.timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (playing) onStop() else onPlay() },
                    enabled = note.onThisPhone
                ) {
                    Icon(
                        imageVector = if (playing) SafeShadeIcons.Pause else SafeShadeIcons.Play,
                        contentDescription = if (playing) "Stop" else "Play",
                        tint = if (note.onThisPhone) colors.ink else colors.inkFaint
                    )
                }
                Waveform(
                    bars = note.waveform,
                    progress = progress,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = clockMs(note.durationMs),
                    style = MaterialTheme.boardType.readout,
                    color = colors.inkMuted
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(state = lamp)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "$word · $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lamp == LampState.TRIP) colors.inkTrip else colors.inkMuted
                )
            }
        }
    }
}

/** The lamp, the word and the line for a note's whereabouts. */
private fun noteState(note: TalkNote, signedIn: Boolean, mine: Boolean): Triple<LampState, String, String> {
    if (!note.onThisPhone) {
        return Triple(LampState.ATTENTION, "On another phone", "Not fetched to this one yet")
    }
    return when (val u = note.uploadState) {
        VoiceUpload.LocalOnly ->
            Triple(LampState.OFF, "On this phone", if (signedIn) "Waiting to send" else "Sign in to share it")
        VoiceUpload.Uploading -> Triple(LampState.ATTENTION, "Sending", "Not with the Circle yet")
        // "Heard" is a fact about this phone's listener, so it is only said
        // of the other person's notes; for one's own the true line is that it
        // is with the Circle.
        is VoiceUpload.Uploaded -> Triple(LampState.LIVE, "On SafeShade Cloud", if (mine) "With the Circle" else if (note.listened) "Heard" else "Not heard yet")
        is VoiceUpload.Failed -> Triple(LampState.TRIP, "Not sent", u.reason)
    }
}

/** "0:07" for 7 000 ms. */
internal fun clockMs(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Preview(name = "Talk", showBackground = true, heightDp = 900)
@Composable
private fun TalkPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            TalkScreen(
                state = TalkUiState(
                    wearerName = "Baba", guardianName = "Dibyendu", signedIn = true,
                    notes = listOf(
                        TalkNote("1", true, "Dibyendu", "09:12", 6_400, List(40) { (it % 7) / 7f }, VoiceUpload.Uploaded("x"), true, true),
                        TalkNote("2", false, "Baba", "09:14", 3_100, List(40) { (it % 5) / 5f }, VoiceUpload.Failed("No internet connection"), false, true)
                    )
                ),
                onHoldStart = {}, onHoldEnd = {}, onPlay = {}, onStop = {}, onOpenSignIn = {}, onBack = {}
            )
        }
    }
}
