package com.safeshade.ui.board

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Elderly
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import com.safeshade.data.DeviceIconType
import com.safeshade.data.PersonaMode
import com.safeshade.ui.icons.SafeShadeIcons

/**
 * Icons for the domain enums.
 *
 * These live in the UI layer rather than on the enums themselves. The previous
 * version hung an `ImageVector` and a `Color` off `PersonaMode`, which put a
 * Compose dependency in the persistence path and gave every mode its own accent
 * hue — seven competing colours across a system whose one rule is that colour
 * means circuit state.
 *
 * So modes are distinguished by icon and nameplate only. That turns out to be
 * plenty: an icon plus an engraved label is how a real panel labels its ways,
 * and it survives greyscale, colour vision deficiency, and dark mode without a
 * second thought.
 *
 * **Why these are still Material when the rest of the app is not.** Everything
 * else has moved to the app's own set. These have not, and it is not an
 * oversight: eight personas and eight device shapes need sixteen distinguishable
 * glyphs, and the custom set covers exactly one of them (`adaptive-mode`, which
 * AUTO now uses). Reusing one custom glyph across several modes would break the
 * only thing telling them apart, which is worse than the mixture. When elderly,
 * child, bicycle, pet, helmet, hand, backpack, umbrella, watch and pendant
 * glyphs exist in `docs/Icons`, this file is the last thing standing between
 * the build and dropping `material-icons-extended` altogether.
 */

val PersonaMode.icon: ImageVector
    get() = when (this) {
        PersonaMode.AUTO -> SafeShadeIcons.AdaptiveMode
        PersonaMode.ELDERLY -> Icons.Outlined.Elderly
        PersonaMode.KIDS -> Icons.Outlined.ChildCare
        PersonaMode.BIKE -> Icons.Outlined.DirectionsBike
        PersonaMode.PET -> Icons.Outlined.Pets
        PersonaMode.HELMET -> Icons.Outlined.School
        PersonaMode.WRIST -> Icons.Outlined.FrontHand
        PersonaMode.BACKPACK -> Icons.Outlined.Backpack
    }

val DeviceIconType.icon: ImageVector
    get() = when (this) {
        DeviceIconType.UMBRELLA -> Icons.Outlined.Umbrella
        DeviceIconType.WATCH -> Icons.Outlined.Watch
        DeviceIconType.BACKPACK -> Icons.Outlined.Backpack
        DeviceIconType.BIKE -> Icons.Outlined.DirectionsBike
        DeviceIconType.PENDANT -> Icons.Outlined.Diamond
        DeviceIconType.HAT -> Icons.Outlined.School
        DeviceIconType.CANE -> Icons.Outlined.Elderly
        DeviceIconType.COLLAR -> Icons.Outlined.Pets
    }
