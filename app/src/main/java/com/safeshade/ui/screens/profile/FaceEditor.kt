package com.safeshade.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import com.safeshade.ui.icons.SafeShadeIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.Avatar
import com.safeshade.ui.board.AvatarCustomiser
import com.safeshade.ui.board.AvatarPresetStrip
import com.safeshade.ui.board.AvatarSpec
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.screens.safety.PlateField
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A name beside a face, then the three ways to a face: a preset from the
 * strip, a photo from the phone, or one made in the customiser.
 *
 * Shared by the profile edit page (you, or the one wearer) and the wearer
 * editor (any person a Guardian looks after), so the two cannot drift. The
 * caller owns the draft; nothing here writes anything.
 */
@Composable
internal fun FaceEditor(
    name: String,
    onNameChange: (String) -> Unit,
    avatarId: String,
    onAvatarChange: (String) -> Unit,
    nameLabel: String,
    namePlaceholder: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var customising by rememberSaveable { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val spec = remember(avatarId) { AvatarSpec.decode(avatarId) ?: AvatarSpec.PRESETS.first() }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { savePhotoAvatar(context, uri) }
            if (saved != null) {
                onAvatarChange(saved)
                photoError = null
            } else {
                photoError = "That photo could not be read. Try another one."
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.gutter)
        ) {
            Avatar(avatarId = avatarId, name = name, size = 72.dp)
            Spacer(Modifier.width(Spacing.lg))
            PlateField(
                label = nameLabel,
                value = name,
                onValueChange = onNameChange,
                placeholder = namePlaceholder,
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "A face", modifier = Modifier.padding(horizontal = Spacing.gutter))
        Spacer(Modifier.height(Spacing.md))
        AvatarPresetStrip(selectedId = avatarId, onSelect = onAvatarChange)
        Spacer(Modifier.height(Spacing.xs))
        Row(modifier = Modifier.padding(horizontal = Spacing.sm)) {
            TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(SafeShadeIcons.CameraAdd, contentDescription = null, tint = colors.inkAttention, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Use a photo", style = MaterialTheme.typography.bodyMedium, color = colors.inkAttention)
            }
            TextButton(onClick = { customising = !customising }) {
                Icon(
                    if (customising) SafeShadeIcons.Tick02 else SafeShadeIcons.Edit,
                    contentDescription = null, tint = colors.inkAttention, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    if (customising) "Done making one" else "Make your own",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkAttention
                )
            }
        }
        val shownError = photoError
        if (shownError != null) {
            Text(
                text = shownError,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkTrip,
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
        }
        AnimatedVisibility(visible = customising) {
            Column {
                Spacer(Modifier.height(Spacing.sm))
                AvatarCustomiser(spec = spec, onChange = { onAvatarChange(it.encode()) })
            }
        }
    }
}
