package com.safeshade.ui.board

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
 * **Sixteen distinct glyphs, all of them the app's own.** This file was the last
 * holdout on Material: eight personas and eight device shapes need sixteen
 * things that cannot be mistaken for one another, and the custom set once
 * covered exactly one of them. It now draws every one - elderly, kid, bicycle,
 * paw, helmet, raised hand, backpack, umbrella, wrist watch, necklace, cap,
 * cane and collar - so nothing here reuses a glyph and nothing here is
 * Material. Dropping `material-icons-extended` from the build was the last step
 * of the same change.
 */

val PersonaMode.icon: ImageVector
    get() = when (this) {
        PersonaMode.AUTO -> SafeShadeIcons.AdaptiveMode
        PersonaMode.ELDERLY -> SafeShadeIcons.Elderly
        PersonaMode.KIDS -> SafeShadeIcons.Kid
        PersonaMode.BIKE -> SafeShadeIcons.Bicycle01
        PersonaMode.PET -> SafeShadeIcons.Paw
        PersonaMode.HELMET -> SafeShadeIcons.Helmet
        PersonaMode.WRIST -> SafeShadeIcons.HandPointingUp
        PersonaMode.BACKPACK -> SafeShadeIcons.Backpack
    }

val DeviceIconType.icon: ImageVector
    get() = when (this) {
        DeviceIconType.UMBRELLA -> SafeShadeIcons.Umbrella
        DeviceIconType.WATCH -> SafeShadeIcons.WristWatch
        DeviceIconType.BACKPACK -> SafeShadeIcons.Backpack
        DeviceIconType.BIKE -> SafeShadeIcons.Bicycle01
        DeviceIconType.PENDANT -> SafeShadeIcons.Necklace
        DeviceIconType.HAT -> SafeShadeIcons.Cap
        DeviceIconType.CANE -> SafeShadeIcons.Cane
        DeviceIconType.COLLAR -> SafeShadeIcons.Collar
    }
