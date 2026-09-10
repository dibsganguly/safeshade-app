package com.safeshade.ui.board

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/**
 * A text input, drawn as a routed channel in the panel.
 *
 * Built on `BasicTextField` rather than Material's `TextField` for one
 * structural reason: the stock field owns a container colour, an animated
 * indicator line and a floating label, all of which are Material's visual
 * language rather than this one. Restyling it into a flat recess means
 * overriding roughly twenty colour slots and still fighting the label
 * animation. Composing the recess directly is less code and cannot drift.
 *
 * [maxLength] is not cosmetic. The firmware parses the Medical ID payload
 * positionally against a negotiated MTU, and `DeviceProtocol.health` caps each
 * field on the way out — a field that lets someone type 200 characters into a
 * 40-character slot is a field that silently discards their work at send time.
 * The caps here mirror that function.
 */
@Composable
fun PlateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    maxLength: Int = 40,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /** Formats the displayed text without changing what is stored. */
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = MaterialTheme.board
    val shape = RoundedCornerShape(Radius.plate)

    Column(modifier = modifier.fillMaxWidth()) {
        Nameplate(label, small = true, muted = true)
        Spacer(Modifier.height(Spacing.xs))

        // The recess is drawn inside `decorationBox` rather than around the
        // field, which is what makes the whole 48dp plate part of the text
        // field's own touch target. Wrapping the field in a Box instead would
        // leave the padding dead to a tap — a hairline-thin target for the
        // person this app is most often read by.
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Grows with the font scale rather than clipping:
                        // heightIn sets a floor, not a fixed height.
                        .heightIn(min = Spacing.touchTarget)
                        .clip(shape)
                        .background(if (enabled) colors.recess else colors.ground)
                        .border(Stroke.hairline, colors.hairline, shape)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.inkFaint,
                            // Hidden from the semantics tree: a screen reader
                            // announcing the placeholder as well as the field
                            // reads the same box twice.
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                    innerTextField()
                }
            }
        )

        // The counter only appears once it is nearly relevant. Showing "0 / 40"
        // under an empty field is noise, and it reads as a demand.
        val nearCap = value.length >= (maxLength * 3) / 4
        if (error != null || helper != null || nearCap) {
            Spacer(Modifier.height(Spacing.xs))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error ?: helper.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) colors.inkTrip else colors.inkFaint,
                    modifier = Modifier.weight(1f)
                )
                if (nearCap) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "${value.length} / $maxLength",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }
    }
}
