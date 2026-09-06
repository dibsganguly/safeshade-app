package com.safeshade.cloud.repo

import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.PairedDevice
import com.safeshade.data.QuickMessage
import com.safeshade.data.UserRole
import com.safeshade.data.Wearer

/**
 * Everything the app would sync, as one immutable value.
 *
 * ### Why the resolver takes this and not the repositories
 *
 * [PayloadResolver] is the part of the push path that can be wrong in a way
 * nobody notices for weeks - a column that never gets sent, a `wearer_id` left
 * null so a contact belongs to nobody, an avatar id that is a local file path
 * being uploaded to a server that cannot fetch it. All of that is a pure
 * function from "what the phone knows" to "what goes on the wire", and a pure
 * function is testable on the JVM with no DataStore, no Android and no
 * Robolectric.
 *
 * So the repositories collapse to this, once, at drain time, and everything
 * downstream is arithmetic. `RepositoryPayloadSource` is the twenty lines that
 * do the collapsing.
 *
 * ### What is deliberately absent
 *
 * `parentalPin`, `smsAllowlist` and `devicePhoneNumber`. They are not omitted
 * from the resolver, they are omitted from the *snapshot*, so there is no
 * expression anywhere downstream that could put them on a wire. A PIN that
 * guards the app on this phone is worth nothing once it is in a database, an
 * allowlist is a list of the wearer's family's telephone numbers, and the
 * device SIM number is the wearer's own. None of the three is needed by any
 * other phone in the circle. handoff7 §6 item 2 says they never enter a
 * `Wearer` and never sync; this is where that is true rather than remembered.
 */
data class SyncSnapshot(
    /** The circle every row is stamped with. Never blank when a push happens. */
    val circleId: String,

    /** The signed-in `auth.users.id`, or null when nobody is signed in. */
    val userId: String? = null,

    /** The account holder's email, for their own `profiles` row. */
    val userEmail: String? = null,

    /** "Me" on the Profile page. See `ProfileSnapshot.ownerName`. */
    val ownerName: String = "",

    val ownerAvatarId: String = "",

    val role: UserRole = UserRole.GUARDIAN,

    val wearers: List<Wearer> = emptyList(),

    /**
     * `SafetySettings.emergencyContacts` - the global list, which is the SOS
     * source and belongs to the circle rather than to one wearer. Rows built
     * from it carry a null `wearer_id`.
     */
    val globalContacts: List<EmergencyContact> = emptyList(),

    val pairedDevices: List<PairedDevice> = emptyList(),

    val alerts: List<FallAlertEvent> = emptyList(),

    val messages: List<QuickMessage> = emptyList(),

    val zones: List<GeofenceZone> = emptyList()
) {

    /** The wearer bound to [address], if any. Devices bind by BLE address. */
    fun wearerForAddress(address: String): Wearer? = wearers.firstOrNull { wearer ->
        wearer.deviceAddresses.any { it.equals(address.trim(), ignoreCase = true) }
    }
}
