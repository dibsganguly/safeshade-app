package com.safeshade.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.BuildConfig
import com.safeshade.data.DarkModePreference
import com.safeshade.data.UserRole
import com.safeshade.ui.board.Avatar
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.BusTick
import com.safeshade.ui.board.ExpandableSection
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PersonRow
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Tile
import com.safeshade.ui.board.TileGrid
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.plateClickable
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.nav.Routes
import com.safeshade.ui.screens.settings.blurb
import com.safeshade.ui.screens.settings.icon
import com.safeshade.ui.screens.settings.label
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** Which person an edit page is about. */
enum class ProfileTarget { OWNER, WEARER }

/** Everything the profile page draws. */
data class ProfileUiState(
    val ownerName: String = "",
    val ownerAvatarId: String = "",
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val wearerAvatarId: String = "",
    val deviceName: String = "SafeShade S1",
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    /** How many reliability checks are failing; every one is a way for a fall alert to silently not arrive. */
    val reliabilityIssueCount: Int = 0,
    val versionName: String = "",
    /** The people a Guardian looks after, as the section lists them. */
    val people: List<ProfilePerson> = emptyList(),
    val showDeveloperOptions: Boolean = BuildConfig.DEBUG,
    /** The account way: lamp, word and line come from the cloud session, never from a guess. */
    val account: AccountWay = AccountWay()
)

/** One person in the "People I look after" bank. */
data class ProfilePerson(
    val id: String,
    val name: String,
    val avatarId: String,
    val detail: String
)

/** How the Profile page reports SafeShade Cloud in one row. */
data class AccountWay(
    val state: LampState = LampState.OFF,
    val label: String = "Off",
    val detail: String = "Saved on this phone only",
    /** False when this build has no project; the row then draws with no chevron and no tap. */
    val tappable: Boolean = false
)

/**
 * The Profile page: who is holding the phone, who they look after, and how
 * the app presents itself.
 *
 * It replaces the Settings hub. The top-right control on the Device page is
 * now this person's face rather than a gear, because the things that used to
 * be "settings" — role, appearance, whether alerts will reach you — are all
 * facts about a person and their phone, and a page that opens on a face says
 * so. Everything that is part of *using* the product still lives on the four
 * main destinations; this page is the person and the phone.
 *
 * A Companion is their own wearer, so the page shows one identity plate. A
 * Guardian sees themselves and, under "People I look after", the wearer.
 * Phase 2 turns that section into a list.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEditOwner: () -> Unit,
    onEditWearer: () -> Unit,
    onOpenPerson: (id: String) -> Unit,
    onAddPerson: () -> Unit,
    onOpenWay: (String) -> Unit,
    onSelectDarkMode: (DarkModePreference) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val guardian = state.role == UserRole.GUARDIAN

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(title = "Profile", onBack = onBack)
        }

        item("identity") {
            IdentityPlate(
                name = if (guardian) state.ownerName else state.wearerName,
                avatarId = if (guardian) state.ownerAvatarId else state.wearerAvatarId,
                line = state.role.blurb,
                placeholder = "Add your name",
                onClick = if (guardian) onEditOwner else onEditWearer
            )
        }

        if (guardian) {
            item("people-heading") { SectionPlate(title = "People I look after") }
            item("people") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    state.people.forEachIndexed { index, person ->
                        if (index > 0) Hairline()
                        PersonRow(
                            name = person.name,
                            avatarId = person.avatarId,
                            detail = person.detail,
                            placeholder = "Name the person who wears it",
                            onClick = { onOpenPerson(person.id) }
                        )
                    }
                    if (state.people.isNotEmpty()) Hairline()
                    Way(
                        name = "Add a person",
                        state = LampState.OFF,
                        stateLabel = "Add",
                        icon = SafeShadeIcons.UserAdd,
                        onClick = onAddPerson
                    )
                }
            }
        }

        item("app-heading") { SectionPlate(title = "This phone") }

        // SafeShade Cloud reports the account's own state and Appearance
        // expands in place, so both stay Ways; the rows that only navigate
        // elsewhere become tiles (2.84).
        item("app-reporting") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "SafeShade Cloud",
                    state = state.account.state,
                    stateLabel = state.account.label,
                    detail = state.account.detail,
                    icon = SafeShadeIcons.CloudBackup,
                    onClick = if (state.account.tappable) { { onOpenWay(Routes.SETTINGS_ACCOUNT) } } else null
                )
                Hairline()
                ExpandableSection(
                    label = "Appearance",
                    icon = SafeShadeIcons.Appearance,
                    count = DarkModePreference.entries.size,
                    preview = listOf(state.darkMode.label to LampState.LIVE)
                ) {
                    DarkModePreference.entries.forEach { option ->
                        Hairline()
                        Way(
                            name = option.label,
                            state = if (option == state.darkMode) LampState.LIVE else LampState.OFF,
                            stateLabel = if (option == state.darkMode) "On" else "Off",
                            detail = option.blurb,
                            icon = option.icon,
                            onClick = { onSelectDarkMode(option) }
                        )
                    }
                }
            }
        }

        item("app") {
            TileGrid(
                tiles = listOf(
                    Tile(
                        title = "Your role",
                        icon = SafeShadeIcons.User,
                        state = LampState.LIVE,
                        stateLabel = state.role.label,
                        onClick = { onOpenWay(Routes.SETTINGS_ROLE) }
                    ),
                    Tile(
                        title = "Alert reliability",
                        icon = SafeShadeIcons.Alert02,
                        // Amber, not red. A missing permission is not an
                        // emergency; it is a thing that will quietly cost you
                        // one later.
                        state = if (state.reliabilityIssueCount > 0) LampState.ATTENTION else LampState.LIVE,
                        stateLabel = if (state.reliabilityIssueCount > 0) "${state.reliabilityIssueCount} to fix" else "All set",
                        onClick = { onOpenWay(Routes.SETTINGS_RELIABILITY) }
                    ),
                    Tile(
                        title = "Privacy",
                        icon = SafeShadeIcons.ShieldWithPadlock,
                        state = LampState.OFF,
                        stateLabel = "Facts",
                        onClick = { onOpenWay(Routes.SETTINGS_PRIVACY) }
                    )
                )
            )
        }

        item("about-heading") { SectionPlate(title = "About") }

        item("about") {
            val aboutTiles = buildList {
                add(
                    Tile(
                        title = "About SafeShade",
                        icon = SafeShadeIcons.Info,
                        state = LampState.OFF,
                        stateLabel = state.versionName.ifBlank { "Version" },
                        onClick = { onOpenWay(Routes.SETTINGS_ABOUT) }
                    )
                )
                if (state.showDeveloperOptions) {
                    add(
                        Tile(
                            title = "Developer",
                            icon = SafeShadeIcons.SourceCode,
                            state = LampState.OFF,
                            stateLabel = "Debug",
                            onClick = { onOpenWay(Routes.SETTINGS_DEVELOPER) }
                        )
                    )
                }
            }
            TileGrid(tiles = aboutTiles)
        }
    }
}

/**
 * The person at the top of the page: a large face, a name, one line, and a
 * chevron. Tapping anywhere on it edits them.
 */
@Composable
private fun IdentityPlate(
    name: String,
    avatarId: String,
    line: String,
    placeholder: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.board
    val shown = name.ifBlank { placeholder }
    // No watermark here: the face is the plate's picture already, and a second
    // glyph behind the name read as clutter (the user asked for it to go).
    BoardPlate(
        modifier = Modifier
            .plateClickable(onClick = onClick)
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$shown. $line. Edit." }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Avatar(avatarId = avatarId, name = name, size = 72.dp)
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shown,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (name.isBlank()) colors.inkMuted else colors.ink
                )
                Spacer(Modifier.height(2.dp))
                Text(text = line, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                imageVector = SafeShadeIcons.ArrowRight01,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(name = "Profile · guardian", showBackground = true)
@Composable
private fun ProfilePreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ProfileScreen(
                state = ProfileUiState(
                    ownerName = "Dibyendu", wearerName = "Baba", reliabilityIssueCount = 1, versionName = "2.6.0", showDeveloperOptions = true,
                    people = listOf(ProfilePerson("1", "Baba", "", "Wears SafeShade S1"))
                ),
                onEditOwner = {}, onEditWearer = {}, onOpenPerson = {}, onAddPerson = {}, onOpenWay = {}, onSelectDarkMode = {}
            )
        }
    }
}

@Preview(name = "Profile · companion, dark", showBackground = true)
@Composable
private fun ProfilePreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ProfileScreen(
                state = ProfileUiState(role = UserRole.COMPANION, wearerName = "Priya", versionName = "2.6.0"),
                onEditOwner = {}, onEditWearer = {}, onOpenPerson = {}, onAddPerson = {}, onOpenWay = {}, onSelectDarkMode = {}
            )
        }
    }
}
