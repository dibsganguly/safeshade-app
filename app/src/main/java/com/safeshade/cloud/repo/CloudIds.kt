package com.safeshade.cloud.repo

import com.safeshade.platform.PhoneNumbers
import com.safeshade.repo.SyncKeys
import java.util.UUID

/**
 * How a local record's identity becomes the uuid the server stores.
 *
 * ### The problem this solves
 *
 * Every primary key in `0001_init.sql` is `uuid`, minted by the client - that
 * is what makes offline-first possible. But the app predates the schema, and
 * three of the things it now has to sync were never given an id at all:
 *
 *  * `EmergencyContact` is `(name, phone, isPrimary, relationship)`.
 *  * `MedicalId` is a bag of strings belonging to a wearer.
 *  * `PairedDevice` is keyed by its BLE address.
 *
 * and one of them has an id that is not a uuid: `PRIMARY_WEARER_ID` is the
 * literal string `"wearer-primary"`, which Postgres rejects outright.
 *
 * Adding an `id` field to those models would work and is the wrong trade: it
 * changes three DataStore shapes and a migration for the benefit of a table
 * nobody has written to yet. Instead the id is *derived*, and derived the same
 * way on every device and on every run.
 *
 * ### Why version-3 name-based UUIDs
 *
 * [UUID.nameUUIDFromBytes] is MD5 over the bytes - deterministic, so the same
 * contact hashes to the same uuid on a cold start, after a reinstall, and on
 * the guardian's second phone. That determinism is the whole point: it is what
 * makes an upsert idempotent. A random id per run would insert the same
 * emergency contact again on every process start until the table was a list of
 * duplicates of one telephone number.
 *
 * MD5 here is not a security decision and nothing depends on it being hard to
 * reverse - a uuid derived from a phone number is not a secret from a person
 * who already has that phone number.
 *
 * ### Namespacing, and why the circle is part of it
 *
 * Every derived id is hashed with a table prefix, so the medical record of
 * wearer X and the wearer row X cannot collide.
 *
 * It is also hashed with the **circle id**, and that half is not cosmetic.
 * `PRIMARY_WEARER_ID` is the literal string `"wearer-primary"` on every fresh
 * install on earth, and 112 is 112 in everybody's contact list. Without the
 * circle in the hash, two unrelated families would derive the *same primary
 * key* for their own primary wearer, their own medical record and their shared
 * emergency number - and `id` is the primary key while `circle_id` is only a
 * column. The second family to sync would upsert onto the first family's row:
 * with row-level security that is a permission error on every attempt until the
 * outbox gives up and tells them their wearer did not reach SafeShade Cloud,
 * and in the one case where the caller is an actor in both circles it is worse,
 * because the row silently moves.
 *
 * Two guardians in *one* circle still derive the same id for the same person,
 * which is exactly right - that is the row they are meant to share.
 */
internal object CloudIds {

    /**
     * A local id as the server will see it.
     *
     * A local id that already is a uuid passes straight through - which is the
     * common case, because `Wearer`, `GeofenceZone`, `FallAlertEvent` and
     * `QuickMessage` all default their id to [UUID.randomUUID]. Anything else -
     * `"wearer-primary"`, and any id a future migration invents - is hashed, so
     * it is stable and it is legal.
     */
    fun cloudId(localId: String, circleId: String): String {
        val trimmed = localId.trim()
        val asUuid = runCatching { UUID.fromString(trimmed) }.getOrNull()
        // toString() round-trips only for a genuine uuid; "1-1-1-1-1" parses
        // and would otherwise pass through in a shape the server did not store.
        if (asUuid != null && asUuid.toString().equals(trimmed, ignoreCase = true)) {
            return asUuid.toString()
        }
        return derive("local", circleId + ":" + trimmed)
    }

    /** The `medical_ids` row id for one wearer. */
    fun medicalId(wearerLocalId: String, circleId: String): String =
        derive("medical", circleId + ":" + cloudId(wearerLocalId, circleId))

    /**
     * The `emergency_contacts` row id for one contact.
     *
     * Keyed on the digits of the phone number rather than the name, because the
     * number is what the app actually dials and what the merge rule unions on
     * (see `MergeRules.contacts`). A contact renamed from "Mum" to "Amma" is
     * the same row; a second number for the same person is a second row, which
     * is correct - they are two things to try in an emergency.
     *
     * @param wearerLocalId null for the global list in `SafetySettings`, which
     *   is the SOS source and belongs to the circle rather than to one person.
     */
    fun contactId(wearerLocalId: String?, phone: String, circleId: String): String {
        val owner = wearerLocalId?.let { cloudId(it, circleId) } ?: GLOBAL
        return derive("contact", circleId + ":" + owner + ":" + PhoneNumbers.digitsOf(phone))
    }

    /**
     * The `devices` row id for one paired BLE address.
     *
     * Circle-namespaced like the rest: one wearable handed on from a parent to
     * a neighbour is two device rows, in two circles, and neither of them
     * upserts over the other.
     */
    fun deviceId(address: String, circleId: String): String =
        derive("device", circleId + ":" + address.trim().lowercase())

    /**
     * The local outbox key for a record that has no id of its own.
     *
     * Deliberately *not* the uuid above. `Outbox.states` is keyed by record id
     * and the UI looks a record up by the id it is holding, so these keys are
     * readable composites rather than hashes - and a `medical_ids` entry and a
     * `wearers` entry for the same person stay two different keys instead of
     * colliding on one.
     */
    fun localKey(prefix: String, vararg parts: String): String =
        (listOf(prefix) + parts).joinToString(":")

    /** The `wearerLocalId` half of a [localKey], or null for the global list. */
    fun ownerOf(localKey: String): String? =
        localKey.split(":").getOrNull(1)?.takeIf { it.isNotBlank() && it != GLOBAL }

    /** One definition, in `repo/`, where the ids are written. */
    const val GLOBAL = SyncKeys.GLOBAL

    private fun derive(namespace: String, value: String): String =
        UUID.nameUUIDFromBytes("safeshade:$namespace:$value".toByteArray()).toString()
}
