package com.safeshade.ui.screens.safety

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.DialControl
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.roundToInt

/** One recording as the list prints it. */
data class EvidenceClipRow(
    val id: String,
    val timeLabel: String,
    val durationMs: Int,
    /** "After a fall", "After an SOS", "Test recording". */
    val causeLabel: String,
    /** "On this phone", "Sending", "On SafeShade Cloud", or "Not sent: <reason>". */
    val whereLabel: String,
    val whereState: LampState,
    val onThisPhone: Boolean
)

/** Everything the evidence page draws. */
data class EvidenceUiState(
    val wearerName: String = "",
    val recordOnFall: Boolean = false,
    val recordOnSos: Boolean = false,
    val durationSeconds: Int = 30,
    val uploadToCloud: Boolean = false,
    val signedIn: Boolean = false,
    val micGranted: Boolean = false,
    /** Non-null while a recording is running: elapsed and total. */
    val recordingElapsedMs: Int? = null,
    val recordingTotalMs: Int? = null,
    /** The last recording attempt's failure, or null. */
    val recordError: String? = null,
    /** The sound meter's newest reading, or null when it is off. */
    val soundDb: Int? = null,
    val meterOn: Boolean = false,
    /** "Loud for 2 min · peak 96 dB" or null when quiet or no data. */
    val loudLine: String? = null,
    /** Printed verbatim under the meter. */
    val calibrationNote: String = "",
    val clips: List<EvidenceClipRow> = emptyList(),
    val playingId: String? = null,
    val playProgress: Float = 0f
)

/**
 * Evidence: the microphone after a fall or an SOS, and the sound around the wearer.
 *
 * Two rockers arm the recording, one dial sets how long it runs, and one
 * rocker decides whether the file ever leaves the phone. Nothing here starts
 * quietly: the phone's own notification shows while the microphone is open,
 * and a recording taken from this page is labelled a test. The meter is an
 * instrument with its limit printed under it, because a phone microphone is
 * not a sound-level meter and the number it gives is a rough one.
 */
@Composable
fun EvidenceScreen(
    state: EvidenceUiState,
    onRecordOnFall: (Boolean) -> Unit,
    onRecordOnSos: (Boolean) -> Unit,
    onDuration: (Int) -> Unit,
    onUploadToCloud: (Boolean) -> Unit,
    onTestRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onMeter: (Boolean) -> Unit,
    onPlay: (id: String) -> Unit,
    onStop: () -> Unit,
    onDelete: (id: String) -> Unit,
    onRequestMic: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val whose = if (state.wearerName.isBlank()) "the wearer" else state.wearerName
    var durationDraft by remember(state.durationSeconds) { mutableFloatStateOf(state.durationSeconds.toFloat()) }
    val recording = state.recordingElapsedMs != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            )
    ) {
        ScreenHeader(
            title = "Evidence",
            subtitle = "What the phone records when something happens",
            onBack = onBack
        )

        if (!state.micGranted) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Microphone",
                    state = LampState.ATTENTION,
                    stateLabel = "Not allowed",
                    detail = "Nothing on this page can record until SafeShade may use the microphone.",
                    icon = SafeShadeIcons.MicrophoneOff,
                    onClick = onRequestMic
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            BoardButton(
                label = "Allow the Microphone",
                icon = SafeShadeIcons.Microphone,
                onClick = onRequestMic,
                weight = ButtonWeight.ATTENTION,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xl))
        }

        SectionPlate(title = "Record when")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "A fall is detected",
                state = if (state.recordOnFall) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.recordOnFall) "On" else "Off",
                detail = "Starts when the countdown ends without a cancel, while the alert is on this phone's screen.",
                icon = SafeShadeIcons.FallDetection,
                checked = state.recordOnFall,
                onCheckedChange = onRecordOnFall
            )
            Hairline()
            Way(
                name = "An SOS is raised",
                state = if (state.recordOnSos) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.recordOnSos) "On" else "Off",
                detail = "Starts when the SOS goes out from this phone, or when the wearable's button raises one while this phone is on the alert.",
                icon = SafeShadeIcons.Microphone,
                checked = state.recordOnSos,
                onCheckedChange = onRecordOnSos
            )
        }
        Spacer(Modifier.height(Spacing.md))
        DialControl(
            label = "Record for",
            value = durationDraft,
            valueRange = 10f..120f,
            step = 10f,
            onValueChange = { durationDraft = it },
            onCommit = { onDuration(durationDraft.roundToInt()) },
            unit = "seconds",
            format = { "${it.roundToInt()} s" },
            advice = { s ->
                when {
                    s <= 20f -> "Catches the first voices. About ${(s * 4).roundToInt()} KB."
                    s <= 60f -> "Long enough to hear whether help arrived. About ${(s * 4).roundToInt()} KB."
                    else -> "Two minutes of a room. About ${(s * 4).roundToInt()} KB per recording."
                }
            }
        )
        Spacer(Modifier.height(Spacing.sm))
        Note(text = "A notification shows the whole time the microphone is open. The recording is saved on this phone first, whatever the switch below says.")

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Where it goes")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "SafeShade Cloud",
                state = when {
                    state.uploadToCloud && state.signedIn -> LampState.LIVE
                    state.uploadToCloud -> LampState.ATTENTION
                    else -> LampState.OFF
                },
                stateLabel = when {
                    state.uploadToCloud && state.signedIn -> "On"
                    state.uploadToCloud -> "Signed out"
                    else -> "Off"
                },
                detail = when {
                    state.uploadToCloud && state.signedIn -> "Each recording is copied to the Circle's private store once it is finished."
                    state.uploadToCloud -> "Recordings stay here until this phone is signed in."
                    else -> "Recordings stay on this phone. Nobody else can hear them."
                },
                icon = SafeShadeIcons.FolderLock,
                checked = state.uploadToCloud,
                onCheckedChange = onUploadToCloud
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Sound around $whose")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(Spacing.lg),
                verticalAlignment = Alignment.Bottom
            ) {
                Readout(
                    label = "ABOUT",
                    value = state.soundDb?.toString() ?: "—",
                    large = true,
                    state = when {
                        state.soundDb == null -> null
                        state.loudLine != null -> LampState.ATTENTION
                        else -> LampState.LIVE
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "dB",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }
            Hairline()
            Way(
                name = "Loud environment",
                state = when {
                    state.loudLine != null -> LampState.ATTENTION
                    state.meterOn -> LampState.LIVE
                    else -> LampState.OFF
                },
                stateLabel = when {
                    state.loudLine != null -> "Loud"
                    state.meterOn -> "Listening"
                    else -> "Off"
                },
                detail = state.loudLine ?: "Warns when a minute of sound stays at 85 dB or more, the level that damages hearing over a working day.",
                icon = SafeShadeIcons.AudioWave,
                checked = state.meterOn,
                onCheckedChange = onMeter
            )
        }
        if (state.calibrationNote.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Note(text = state.calibrationNote)
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Recordings")
        Spacer(Modifier.height(Spacing.sm))
        if (recording) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Recording",
                    state = LampState.TRIP,
                    stateLabel = "${(state.recordingElapsedMs ?: 0) / 1000} s",
                    detail = "Stops by itself at ${(state.recordingTotalMs ?: 0) / 1000} s.",
                    icon = SafeShadeIcons.Microphone
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            BoardButton(
                label = "Stop Now",
                onClick = onStopRecording,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            BoardButton(
                label = "Make a Test Recording",
                supporting = "Ten seconds, saved like a real one and labelled a test.",
                icon = SafeShadeIcons.Microphone,
                onClick = onTestRecording,
                enabled = state.micGranted,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.recordError != null) {
            Spacer(Modifier.height(Spacing.sm))
            FailureNote(text = state.recordError)
        }
        Spacer(Modifier.height(Spacing.md))
        if (state.clips.isEmpty()) {
            Note(text = "No recordings. Each one appears here with where it is and a play button.")
        } else {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.clips.forEachIndexed { i, clip ->
                    if (i > 0) Hairline()
                    val playing = state.playingId == clip.id
                    Way(
                        name = "${clip.causeLabel} · ${clip.timeLabel}",
                        state = clip.whereState,
                        stateLabel = if (playing) "Playing" else "${(clip.durationMs / 1000).coerceAtLeast(1)} s",
                        detail = clip.whereLabel,
                        icon = if (playing) SafeShadeIcons.Pause else SafeShadeIcons.Play,
                        onClick = if (clip.onThisPhone) ({ if (playing) onStop() else onPlay(clip.id) }) else null
                    )
                    if (playing) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.End) {
                            BoardButton(
                                label = "Delete",
                                onClick = { onDelete(clip.id) },
                                weight = ButtonWeight.QUIET,
                                icon = SafeShadeIcons.DeleteBin
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Note(text = "Tap a recording to hear it; while it plays, Delete removes it from this phone. A copy already on SafeShade Cloud stays there.")
        }
    }
}

@Preview(name = "Evidence", showBackground = true, heightDp = 1600)
@Composable
private fun EvidencePreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            EvidenceScreen(
                state = EvidenceUiState(
                    wearerName = "Baba",
                    recordOnFall = true,
                    micGranted = true,
                    soundDb = 62,
                    meterOn = true,
                    calibrationNote = "Uncalibrated: a phone microphone is not a sound-level meter. Read this to within about 10 dB.",
                    clips = listOf(
                        EvidenceClipRow("1", "Today 14:02", 30_000, "After a fall", "On this phone", LampState.OFF, true),
                        EvidenceClipRow("2", "Mon 09:12", 10_000, "Test recording", "On SafeShade Cloud", LampState.LIVE, true)
                    )
                ),
                onRecordOnFall = {}, onRecordOnSos = {}, onDuration = {}, onUploadToCloud = {},
                onTestRecording = {}, onStopRecording = {}, onMeter = {}, onPlay = {}, onStop = {},
                onDelete = {}, onRequestMic = {}, onBack = {}
            )
        }
    }
}
