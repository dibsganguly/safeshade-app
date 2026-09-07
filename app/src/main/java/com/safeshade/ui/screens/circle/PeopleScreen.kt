package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PersonRow
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** One person, as the list draws them. */
data class PersonListRow(
    val id: String,
    val name: String,
    val avatarId: String,
    /** "Wears SafeShade S1 · Elderly", or "No wearable yet". */
    val detail: String,
    val state: LampState,
    val stateLabel: String,
    val isSelf: Boolean = false
)

/**
 * People I look after.
 *
 * A Guardian's list of the people whose wearables this phone watches. Each
 * row is a person as a way: face, name, which wearable and which mode, and
 * a state word for the link to that wearable. Tapping a row opens the
 * editor; the one button on the page adds a person. A Companion is their
 * own single wearer and sees themselves alone with no add.
 */
@Composable
fun PeopleScreen(
    role: UserRole,
    people: List<PersonListRow>,
    onOpen: (id: String) -> Unit,
    onAdd: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val guardian = role == UserRole.GUARDIAN

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(
                title = if (guardian) "People I look after" else "You",
                subtitle = if (guardian) "Each person, their wearable, and how it is set up"
                else "Your wearable, your medical ID, your own contacts",
                onBack = onBack
            )
        }

        item("people") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                if (people.isEmpty()) {
                    Text(
                        text = "Nobody yet. Add the person who wears the device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.lg)
                    )
                }
                people.forEachIndexed { index, person ->
                    if (index > 0) Hairline()
                    PersonRow(
                        name = person.name,
                        avatarId = person.avatarId,
                        detail = person.detail,
                        state = person.state,
                        stateLabel = person.stateLabel,
                        placeholder = if (person.isSelf) "Add your name" else "Name this person",
                        onClick = { onOpen(person.id) }
                    )
                }
            }
        }

        if (guardian) {
            item("add") {
                BoardButton(
                    label = "Add a Person",
                    icon = SafeShadeIcons.UserAdd,
                    onClick = onAdd,
                    weight = ButtonWeight.COMMIT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(name = "People", showBackground = true)
@Composable
private fun PeoplePreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            PeopleScreen(
                role = UserRole.GUARDIAN,
                people = listOf(
                    PersonListRow("1", "Baba", "", "Wears SafeShade S1 · Elderly", LampState.OFF, "Off"),
                    PersonListRow("2", "Mia", "", "No wearable yet", LampState.UNKNOWN, "Unbound")
                ),
                onOpen = {}, onAdd = {}, onBack = {}
            )
        }
    }
}
