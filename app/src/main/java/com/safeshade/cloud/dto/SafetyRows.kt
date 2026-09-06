package com.safeshade.cloud.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape of the safety tables — the ones that carry what actually
 * happened to somebody.
 *
 * The nullable-with-default rule, the string-typed timestamps and the
 * denormalised `circle_id` are all explained once in `CircleRows.kt`. Read that
 * first; this file only adds the reasons specific to these tables.
 */

/**
 * The Medical ID, one row per wearer.
 *
 * The field order deliberately matches `data/Models.kt`'s `MedicalId`, which in
 * turn matches the eleven positional fields the firmware's `HealthCallbacks`
 * parses off `HEALTH_CHAR`. Three representations of one record, and keeping
 * them in the same order is the cheapest way to notice when one of them gains a
 * field the others have not.
 *
 * Note there is no `circle_id`-free variant: a Medical ID belongs to a circle,
 * so every guardian in it can show a paramedic the card. That is the entire
 * point of the feature and it is why the QR card exists.
 */
@Serializable
data class MedicalIdRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("blood_type") val bloodType: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("allergies") val allergies: String? = null,
    @SerialName("age") val age: Int? = null,
    @SerialName("conditions") val conditions: String? = null,
    @SerialName("medications") val medications: String? = null,
    @SerialName("secondary_contact_name") val secondaryContactName: String? = null,
    @SerialName("secondary_contact") val secondaryContact: String? = null,
    @SerialName("organ_donor") val organDonor: Boolean? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * Somebody to reach when things go wrong.
 *
 * [phone] is stored exactly as the guardian typed it, **not normalised on the
 * way in**. The app has a hard-won position on this: a stored number that has
 * been silently "corrected" is a number nobody can audit, and this project
 * already has one contact on a test device carrying eleven digits after the
 * country code with no way to tell which digit is the extra one. Normalisation
 * belongs at the point of dialling, where a failure is visible.
 */
@Serializable
data class EmergencyContactRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("relationship") val relationship: String? = null,
    /** Lower is contacted first. */
    @SerialName("priority") val priority: Int? = null,
    @SerialName("notify_by_sms") val notifyBySms: Boolean? = null,
    @SerialName("notify_by_email") val notifyByEmail: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A fall, an SOS, a missed check-in — the trip log, in the cloud.
 *
 * [kind] and [outcome] carry the *names* of `TripKind` and `TripOutcome`, never
 * their ordinals. `data/local/Dtos.kt` gives the reason at length: `PersonaMode`
 * gained `AUTO` at index 0 and every ordinal after it moved. A stored ordinal
 * would have silently re-pointed every historical alert at the wrong kind, and
 * a trip log that misreports what happened is worse than no trip log.
 */
@Serializable
data class AlertRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    /** `FALL` | `SOS` | `PHONE_SOS` | `MISSED_CHECKIN` | `ZONE_EXIT` | `JOURNEY_OVERDUE`. */
    @SerialName("kind") val kind: String? = null,
    /** `PENDING` | `DISMISSED` | `CONTACTED` | `AUTO_RESOLVED`. */
    @SerialName("outcome") val outcome: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    /** The human-readable place string the phone had at the time. */
    @SerialName("location_label") val locationLabel: String? = null,
    /** Telemetry snapshot, free text, exactly as the trip log shows it. */
    @SerialName("note") val note: String? = null,
    @SerialName("was_emergency_contacted") val wasEmergencyContacted: Boolean? = null,
    @SerialName("acknowledged_by") val acknowledgedBy: String? = null,
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * One attempt to tell one person about one alert.
 *
 * This table is the reason the whole cloud tier is worth building, and it is
 * written **only by the server**, never by a phone.
 *
 * The app's existing honesty problem is stated plainly in the handoff:
 * `ActionResult.Sent` means "handed to the radio", not "delivered", because
 * `sendMultipartTextMessage` is called with null sent/delivery intents, and BLE
 * writes have no application-level ack. Nothing in the on-device system can
 * honestly draw a delivery receipt.
 *
 * An email sent from an edge function *can*. Resend returns a 2xx with an id or
 * it does not, and `send-alert-email` writes exactly what it was told:
 * [status] is `sent` only after that 2xx. `failed` carries Resend's own message
 * in [error]. `unknown` exists for the one genuinely ambiguous case — the
 * request left the function and the process died or the call threw after the
 * bytes went out — and it must never be rendered as either success or failure.
 */
@Serializable
data class AlertDeliveryRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("alert_id") val alertId: String? = null,
    /** Email address or phone number, as it was actually used. */
    @SerialName("recipient") val recipient: String? = null,
    /** `email` | `sms` | `push`. */
    @SerialName("channel") val channel: String? = null,
    /** `queued` | `sent` | `failed` | `unknown`. Constrained in SQL. */
    @SerialName("status") val status: String? = null,
    /** The provider's own message on failure. Never shown raw to a user. */
    @SerialName("error") val error: String? = null,
    /** Resend's message id, when there is one. The audit trail. */
    @SerialName("provider_id") val providerId: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A Guardian to Companion message, or a reply coming back.
 *
 * [fromGuardian] is the direction, and it is not cosmetic. On the BLE side the
 * two directions are two different characteristics with two different firmware
 * handlers — writing a reply to the message characteristic makes the wearable
 * buzz at its own wearer with their own reply, which this app has shipped once.
 * Keeping the direction as an explicit field here means a message replayed out
 * of the cloud onto a fresh phone cannot lose which way it was going.
 */
@Serializable
data class MessageRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("from_guardian") val fromGuardian: Boolean? = null,
    /** `BLE` | `SMS` | `CLOUD`. How it actually travelled. */
    @SerialName("channel") val channel: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/** A safe zone. Mirrors `GeofenceZone`. */
@Serializable
data class ZoneRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("radius_meters") val radiusMeters: Double? = null,
    @SerialName("alert_on_exit") val alertOnExit: Boolean? = null,
    @SerialName("alert_on_enter") val alertOnEnter: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/** A crossing of a zone boundary. */
@Serializable
data class ZoneEventRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("zone_id") val zoneId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    /** `enter` | `exit`. */
    @SerialName("kind") val kind: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A pointer to something in a private storage bucket.
 *
 * The bytes live in Storage; only [storagePath] lives here. The row is what
 * row-level security protects and what the pull protocol syncs, so a guardian
 * who joins the circle later can see that evidence exists and fetch a signed
 * URL for it, without the object itself ever being publicly addressable.
 */
@Serializable
data class EvidenceRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("alert_id") val alertId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    /** `audio` | `photo` | `note`. */
    @SerialName("kind") val kind: String? = null,
    @SerialName("bucket") val bucket: String? = null,
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("byte_size") val byteSize: Long? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
