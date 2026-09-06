package com.safeshade.data

import com.safeshade.platform.PhoneNumbers

/**
 * The pure half of the multi-wearer model: resolution, contact union, and the
 * rules about what a list of wearers is allowed to look like.
 *
 * Everything here is a total function over plain data with no coroutine, no
 * DataStore and no Android in sight, which is the point. The two questions
 * this file answers — "whose device just connected?" and "who does an SOS
 * actually reach?" — are the two that are hardest to observe at runtime and
 * the two whose wrong answers are worst: the first sends one person's medical
 * card to another person's wearable, the second silently shortens the list of
 * people an emergency reaches. Both are unit-tested against this file rather
 * than against a repository.
 */

/**
 * The id of the wearer that an upgraded install already had.
 *
 * A constant rather than a fresh UUID, and that is load-bearing. The `wearers`
 * flow synthesises wearer #1 on every emission when the key is absent; if that
 * synthesis minted a random id, no two emissions would agree on it, nothing
 * could select or bind to it, and the id the v2 to v3 migration wrote down
 * would not match the one screens had already seen.
 */
const val PRIMARY_WEARER_ID = "wearer-primary"

/**
 * The outcome of a change to the wearer list.
 *
 * A result rather than an exception because every caller of these is a tap on
 * a control, and the honest response to "remove the only person on the list"
 * is a sentence in the UI, not a crash. [Refused.reason] is written to be
 * shown verbatim.
 */
sealed interface WearerResult {

    data class Ok(val wearers: List<Wearer>) : WearerResult

    /** The change was not made. [reason] says why, in the user's words. */
    data class Refused(val reason: String) : WearerResult
}

/**
 * Which wearer is on the other end of the link.
 *
 * **The connected wearer is not the selected wearer.** A guardian looking at
 * Ma's page while Baba's cane is the thing actually connected is the ordinary
 * case, not the edge case, and reading the selected wearer at a push site
 * would send Ma's medical card to Baba's device.
 *
 * Resolution order:
 *  1. the wearer whose [Wearer.deviceAddresses] contains [address], compared
 *     case-insensitively because Android reports addresses upper-case while a
 *     hand-typed or restored one may not be;
 *  2. the selected wearer, when the address matches nobody — including when
 *     there is no link at all and the address is blank;
 *  3. the first wearer, which for a COMPANION install is the only one.
 *
 * Returns null only for an empty list, which the `wearers` flow makes
 * impossible by synthesising wearer #1. A caller that still sees null has a
 * genuinely empty store and must not invent a person to fill it.
 */
fun resolveWearerForDevice(
    wearers: List<Wearer>,
    address: String?,
    selectedWearerId: String? = null
): Wearer? {
    if (wearers.isEmpty()) return null
    val wanted = address?.trim().orEmpty()
    if (wanted.isNotBlank()) {
        val bound = wearers.firstOrNull { wearer ->
            wearer.deviceAddresses.any { it.equals(wanted, ignoreCase = true) }
        }
        if (bound != null) return bound
    }
    return wearers.firstOrNull { it.id == selectedWearerId } ?: wearers.first()
}

/**
 * Everyone an alert for [wearer] should reach.
 *
 * The global [SafetySettings.emergencyContacts] is the SOS source and stays
 * that way; [Wearer.contacts] supplements it. The union is deliberately in
 * that order — global first — because the global list is the one the user
 * curated before wearers existed and the one whose first entry
 * [SafetySettings.primaryContact] picks as the number to dial.
 *
 * Duplicates are collapsed on the *normalised* number, not the stored string:
 * the same aunt saved once as `9830011223` and once as `+91 98300 11223` is
 * one person, and texting her twice during an emergency is both alarming and
 * a way to hit a carrier's rate limit. Name and relationship are not part of
 * the key — two different names for one number are still one phone.
 *
 * A wearer with no extra contacts, or a null wearer, returns the global list
 * unchanged, so every call site behaves exactly as it did before this
 * function existed until somebody actually adds a per-person contact.
 */
fun SafetySettings.contactsFor(wearer: Wearer?): List<EmergencyContact> {
    val extra = wearer?.contacts.orEmpty()
    if (extra.isEmpty()) return emergencyContacts
    val seen = emergencyContacts.mapTo(mutableSetOf()) { contactKey(it) }
    val merged = emergencyContacts.toMutableList()
    for (contact in extra) {
        val key = contactKey(contact)
        // A blank number is not a contact and must never become the key that
        // swallows every other blank one.
        if (key.isBlank()) continue
        if (seen.add(key)) merged += contact
    }
    return merged
}

private fun contactKey(contact: EmergencyContact): String =
    PhoneNumbers.digitsOf(contact.phone)

/**
 * Applies the role fork to a wearer list.
 *
 * A COMPANION is their own wearer, so exactly one entry carries
 * [Wearer.isSelf]. Switching to COMPANION does **not** delete the other
 * wearers: somebody who set the app up as a guardian, added three people and
 * then changed their mind about the role would lose three medical IDs, and no
 * amount of "exactly one" in a spec justifies deleting a person's allergies.
 * The first wearer becomes self and is named from the owner if it has no name
 * of its own; the rest are simply no longer self.
 *
 * A GUARDIAN's wearer #1 is the person they look after, never themselves, so
 * every [Wearer.isSelf] is cleared.
 */
fun applyRoleFork(wearers: List<Wearer>, role: UserRole, ownerName: String): List<Wearer> {
    if (wearers.isEmpty()) return wearers
    return when (role) {
        UserRole.COMPANION -> wearers.mapIndexed { index, wearer ->
            if (index == 0) {
                wearer.copy(
                    isSelf = true,
                    name = wearer.name.ifBlank { ownerName.trim() }
                )
            } else {
                wearer.copy(isSelf = false)
            }
        }

        UserRole.GUARDIAN -> wearers.map { it.copy(isSelf = false) }
    }
}
