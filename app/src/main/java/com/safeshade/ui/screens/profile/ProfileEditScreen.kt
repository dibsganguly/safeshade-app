package com.safeshade.ui.screens.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.AvatarSpec
import com.safeshade.ui.board.EditorFootBar
import com.safeshade.ui.board.EditorScaffold
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import java.io.File
import java.util.UUID

/** Everything the edit page draws. */
data class ProfileEditUiState(
    val target: ProfileTarget = ProfileTarget.OWNER,
    val name: String = "",
    val avatarId: String = ""
)

/**
 * Name and face for one person: the account holder or the wearer.
 *
 * Three ways to a face, in the order people reach for them: a preset from
 * the strip, a photo from the phone, or a face made in the customiser. A
 * photo is copied app-private the moment it is picked, so the id never
 * points at a permission that expires. Nothing is written until Save; the
 * page holds a draft and the caller commits it.
 */
@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    onSave: (name: String, avatarId: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    var name by rememberSaveable { mutableStateOf(state.name) }
    var avatarId by rememberSaveable { mutableStateOf(state.avatarId) }
    val owner = state.target == ProfileTarget.OWNER
    val dirty = name.trim() != state.name || avatarId != state.avatarId

    // The editor's foot is pinned (candidate 2.34), the same shape every
    // editor in this app uses: Save amber, Discard beside it in trip ink.
    EditorScaffold(
        modifier = modifier.fillMaxSize().background(colors.ground),
        bottomPadding = contentPadding.calculateBottomPadding(),
        foot = {
            EditorFootBar(
                primaryLabel = "Save",
                onPrimary = { onSave(name.trim(), avatarId) },
                secondaryLabel = "Discard",
                onSecondary = { onBack?.invoke() },
                changedLine = if (dirty) "Unsaved changes" else "Nothing to save",
                statusState = if (dirty) LampState.ATTENTION else null,
                primaryEnabled = name.isNotBlank() && dirty,
                secondaryEnabled = dirty
            )
        }
    ) { footPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding() + Spacing.sm,
                    bottom = footPadding.calculateBottomPadding() + Spacing.lg
                )
        ) {
            ScreenHeader(
                title = if (owner) "You" else "Who wears it",
                subtitle = if (owner) "Your name and face, as your Circle sees them."
                else "Their name is on the board and on the emergency card.",
                onBack = onBack,
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
            Spacer(Modifier.height(Spacing.lg))

            FaceEditor(
                name = name,
                onNameChange = { name = it },
                avatarId = avatarId,
                onAvatarChange = { avatarId = it },
                nameLabel = if (owner) "Your name" else "Their name",
                namePlaceholder = if (owner) "Priya" else "Baba"
            )
        }
    }
}

/**
 * Copies a picked photo into `files/avatars/` as a 512px JPEG and returns
 * its avatar id, or null if the picture could not be decoded.
 *
 * Decoded through `inSampleSize` first so a 12-megapixel photo never lands
 * in memory at full size, then scaled to 512 on its short side and cropped
 * square, which is what a round avatar shows anyway.
 */
fun savePhotoAvatar(context: Context, uri: Uri): String? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    val side = minOf(bitmap.width, bitmap.height)
    val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
    val scaled = if (side > 512) Bitmap.createScaledBitmap(square, 512, 512, true) else square
    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    "photo:${file.name}"
}.getOrNull()

@Preview(name = "Edit · owner", showBackground = true)
@Composable
private fun EditPreview() {
    SafeShadeTheme {
        ProfileEditScreen(state = ProfileEditUiState(name = "Dibyendu", avatarId = AvatarSpec.PRESETS[0].encode()), onSave = { _, _ -> })
    }
}
