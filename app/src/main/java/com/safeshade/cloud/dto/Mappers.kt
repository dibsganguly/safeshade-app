package com.safeshade.cloud.dto

import com.safeshade.cloud.parseServerInstant
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.MedicalId
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Domain to wire, and back.
 *
 * Three pairs are implemented here — [FallAlertEvent], [MedicalId] and
 * [GeofenceZone] — as the worked examples for the rest. Phase 2 adds the others
 * in the same shape.
 *
 * ### Two rules, and both of them are about not losing data
 *
 * **1. `toDomain()` is total.** It never throws, never returns null, and never
 * propagates a parse failure. These functions run inside `map` on flows that
 * feed the whole UI; the codebase's existing scar tissue on this is explicit —
 * a `JsonSyntaxException` thrown inside a `map` on `dataStore.data` does not
 * fail one read, it cancels the flow for every collector in the process and the
 * app renders empty forever with no crash to report. A cloud row is *more*
 * untrusted than a local one, not less: it was written by a different build of
 * this app, on somebody else's phone, possibly months ago.
 *
 * **2. Enums cross the wire by name.** [enumOrDefault] mirrors the one in
 * `data/local/Dtos.kt` and exists for the same reason: an ordinal silently
 * re-points at a different constant the moment anyone inserts an entry, which
 * `PersonaMode` did by gaining `AUTO` at index 0. A name this build does not
 * recognise degrades to a documented default rather than throwing.
 *
 * ### Why the domain types are not simply annotated `@Serializable`
 *
 * Because `data/Models.kt` is already round-tripped by Gson through
 * `data/local/Dtos.kt`, and putting `@Serializable` on a class Gson also
 * handles gives one class two reflection-driven serializers with different
 * rules about defaults and nulls — which is a whole afternoon of debugging the
 * first time a field disagrees. The domain layer stays free of both.
 */

/** Safe enum decode by name. Never throws. See the file KDoc. */
private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
    val raw = name ?: return default
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}

/**
 * Epoch millis to an ISO-8601 instant string, which is what Postgres accepts
 * for a `timestamptz` and what PostgREST hands back.
 */
internal fun Long.toIsoOrNull(): String? =
    runCatching { Instant.ofEpochMilli(this).toString() }.getOrNull()

/**
 * ISO-8601 back to epoch millis, or [fallback].
 *
 * Postgres returns `timestamptz` in a handful of shapes depending on the
 * driver and the column's precision, and one of them — a space instead of the
 * `T`, and an offset written `+00` rather than `Z` — is not ISO-8601 at all.
 * Rather than accumulate format patterns, this tries the strict parse and falls
 * back to a caller-supplied value. A trip whose timestamp will not parse is
 * still a trip that happened; dropping the row would be the worse error.
 */
internal fun String?.isoToEpochMillis(fallback: Long): Long {
    if (this.isNullOrBlank()) return fallback
    return parseServerInstant(this)?.toEpochMilli() ?: fallback
}

// ============================================
// FallAlertEvent <-> alerts
// ============================================

/**
 * @param circleId and [wearerId] are not on the domain type. They are context
 *   the caller holds, not facts about the event, and threading them in as
 *   parameters keeps `data/Models.kt` free of any notion that a cloud exists.
 */
fun FallAlertEvent.toRow(circleId: String, wearerId: String? = null): AlertRow = AlertRow(
    id = id,
    circleId = circleId,
    wearerId = wearerId,
    kind = kind.name,
    outcome = outcome.name,
    occurredAt = timestamp.toIsoOrNull(),
    locationLabel = location,
    note = note,
    wasEmergencyContacted = wasEmergencyContacted
)

fun AlertRow.toDomain(): FallAlertEvent = FallAlertEvent(
    id = id ?: UUID.randomUUID().toString(),
    // Falling back to "now" would date a two-week-old fall to this morning and
    // reorder the trip log around it. Zero is visibly wrong instead, which is
    // the point: a wrong timestamp must look wrong.
    timestamp = occurredAt.isoToEpochMillis(fallback = 0L),
    kind = enumOrDefault(kind, TripKind.FALL),
    outcome = enumOrDefault(outcome, TripOutcome.PENDING),
    wasEmergencyContacted = wasEmergencyContacted ?: false,
    location = locationLabel,
    note = note
)

// ============================================
// MedicalId <-> medical_ids
// ============================================

fun MedicalId.toRow(
    circleId: String,
    wearerId: String? = null,
    rowId: String
): MedicalIdRow = MedicalIdRow(
    // A Medical ID has no id of its own in the domain model - there is one per
    // wearer and the app has never needed to name it. The caller supplies a
    // stable id (the wearer's, in practice) so that repeated syncs upsert the
    // same row instead of accumulating one card per save.
    id = rowId,
    circleId = circleId,
    wearerId = wearerId,
    bloodType = bloodType,
    emergencyContact = emergencyContact,
    contactName = contactName,
    allergies = allergies,
    age = age,
    conditions = conditions,
    medications = medications,
    secondaryContactName = secondaryContactName,
    secondaryContact = secondaryContact,
    organDonor = organDonor,
    notes = notes
)

fun MedicalIdRow.toDomain(): MedicalId = MedicalId(
    // Blank, not null: every field on MedicalId is a non-null String with a ""
    // default, and `isUsable` / `filledFieldCount` both count blanks. Turning a
    // missing column into "" keeps those two honest.
    bloodType = bloodType.orEmpty(),
    emergencyContact = emergencyContact.orEmpty(),
    contactName = contactName.orEmpty(),
    allergies = allergies.orEmpty(),
    age = age ?: 0,
    conditions = conditions.orEmpty(),
    medications = medications.orEmpty(),
    secondaryContactName = secondaryContactName.orEmpty(),
    secondaryContact = secondaryContact.orEmpty(),
    organDonor = organDonor ?: false,
    notes = notes.orEmpty()
)

// ============================================
// GeofenceZone <-> zones
// ============================================

fun GeofenceZone.toRow(circleId: String, wearerId: String? = null): ZoneRow = ZoneRow(
    id = id,
    circleId = circleId,
    wearerId = wearerId,
    name = name,
    lat = lat,
    lon = lon,
    // Float on the domain side, double precision in Postgres. Widening is
    // lossless; the narrowing on the way back is the direction to watch.
    radiusMeters = radiusMeters.toDouble(),
    alertOnExit = alertOnExit,
    alertOnEnter = alertOnEnter
)

fun ZoneRow.toDomain(): GeofenceZone = GeofenceZone(
    id = id ?: UUID.randomUUID().toString(),
    name = name.orEmpty(),
    lat = lat ?: 0.0,
    lon = lon ?: 0.0,
    // A zone with no radius would match nothing and silently stop alerting, so
    // it takes the same 200m default a newly-drawn zone gets rather than 0.
    radiusMeters = radiusMeters?.toFloat() ?: 200f,
    alertOnExit = alertOnExit ?: true,
    alertOnEnter = alertOnEnter ?: false
)
