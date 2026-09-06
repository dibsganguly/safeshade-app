package com.safeshade.cloud.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape of the circle-and-account tables.
 *
 * ### Every field is nullable, with a default. All of them, always.
 *
 * `data/local/Dtos.kt` gives one reason for this rule — Gson never runs Kotlin
 * constructors, so defaults never happen on decode. These DTOs are decoded by
 * kotlinx.serialization instead, which *does* honour defaults, so the reason
 * here is a different one and just as sharp:
 *
 *  1. **The server schema moves without the app.** A column added in a
 *     migration arrives in the JSON of a client that has never heard of it, and
 *     a column not yet added is absent from a client that expects it. The
 *     client is configured with `ignoreUnknownKeys = true` for the first case;
 *     defaults cover the second. Without them, one migration bricks decode for
 *     every phone that has not updated.
 *  2. **PostgREST returns only the columns you selected.** A narrowed `select`
 *     produces an object missing most of the class's fields, and a non-null
 *     field turns that into a `MissingFieldException` inside a coroutine rather
 *     than a partially-filled row.
 *  3. **`explicitNulls = false` on the encoder side** means a null field is
 *     omitted from the outgoing body rather than sent as JSON `null`. On an
 *     upsert that is the difference between "I have nothing to say about this
 *     column" and "erase this column" — and a phone that has only ever known
 *     half a row must not erase the other half on its first sync.
 *
 * ### Why timestamps, ids and enums are all `String?`
 *
 * Postgres hands back `timestamptz` as ISO-8601 text and `uuid` as text.
 * Binding them to a date type here would tie this file to whichever
 * `Instant` the current supabase-kt happens to expose — a moving target across
 * its 2.x/3.x line, and a dependency the domain layer does not need. Parsing
 * happens once, at the [Mappers] boundary, where a malformed value can be
 * turned into a sane default instead of throwing out of a decode.
 *
 * ### Ids are minted by the client
 *
 * Every `id` is a UUID the phone generates before the row exists. That is what
 * makes offline-first possible at all: an alert raised in a basement has to be
 * referenceable by its evidence upload, its deliveries and its trip-log entry
 * long before any server has seen it.
 *
 * ### `circle_id` is denormalised onto every circle-scoped row
 *
 * Not for query convenience — for row-level security. Every policy is
 * `is_circle_member(circle_id)`, and a policy that had to join to find the
 * circle would run that join on every row of every read.
 */
@Serializable
data class ProfileRow(
    /** Equal to `auth.users.id`. Not client-minted; the only such id here. */
    @SerialName("id") val id: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_id") val avatarId: String? = null,
    /** `guardian` or `companion` - mirrors the app's `UserRole`. */
    @SerialName("role") val role: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("locale") val locale: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A Circle: the group of people around one wearer.
 *
 * "Circle" rather than "family" or "account" because that is already the name
 * of the tab, and the vocabulary the app shows a user should be the vocabulary
 * its tables use. A circle can be a family, a care team or one person alone.
 */
@Serializable
data class CircleRow(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * One person's membership of one circle.
 *
 * [role] is the authorisation model in its entirety: `owner` can invite and
 * remove, `guardian` can act (acknowledge alerts, message, change settings),
 * `viewer` can only look.
 *
 * All three are enforced in SQL, not here — a client-side role check is a UI
 * convenience, never a security boundary. Specifically: reads use
 * `is_circle_member`, writes use `is_circle_actor` (owner or guardian only),
 * and membership itself is owner-write with one narrow bootstrap exception.
 * Joining a circle goes through `accept_invite(token)`, because a policy that
 * let a person insert their own membership row would let anybody who has ever
 * seen a circle id add themselves back as an owner.
 */
@Serializable
data class CircleMemberRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    /** `owner` | `guardian` | `viewer`. Checked by a SQL constraint. */
    @SerialName("role") val role: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    /**
     * Which avatar the member picked, by id rather than by URL.
     *
     * The set is a fixed gallery shipped in the app, so storing an id keeps a
     * member's face working when the artwork is redrawn, and keeps a redraw
     * from being a data migration.
     */
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * The person a circle exists for.
 *
 * Separate from [ProfileRow] because a wearer very often has no account at all
 * — the elderly-parent case the product is built around is a guardian's phone
 * and a wearable, with the wearer never opening the app. Attaching wearer data
 * to `auth.users` would make an account a precondition for being cared for.
 */
@Serializable
data class WearerRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("name") val name: String? = null,
    /** Optional link to an account, when the wearer does use the app. */
    @SerialName("user_id") val userId: String? = null,
    @SerialName("persona_mode") val personaMode: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/** A physical wearable, as the cloud knows it. */
@Serializable
data class DeviceRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("name") val name: String? = null,
    /** `s1` | `5g` | `spark`. The three product lines in the deck. */
    @SerialName("model") val model: String? = null,
    /** BLE MAC. Kept because it is how the phone re-finds the device. */
    @SerialName("ble_address") val bleAddress: String? = null,
    @SerialName("serial") val serial: String? = null,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    @SerialName("icon_type") val iconType: String? = null,
    @SerialName("battery_percent") val batteryPercent: Int? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A paid plan, scoped to a user rather than to a circle.
 *
 * User-scoped deliberately: the deck sells Free / Plus / Pro to a person, and a
 * guardian who pays should keep their tier when they leave one circle and join
 * another. What the tier unlocks *within* a circle is a read of the owner's
 * subscription, which is a policy question, not a schema one.
 */
@Serializable
data class SubscriptionRow(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    /** `free` | `plus` | `pro`. */
    @SerialName("tier") val tier: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("renews_at") val renewsAt: String? = null,
    @SerialName("provider") val provider: String? = null,
    @SerialName("provider_ref") val providerRef: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A pending invitation to join a circle.
 *
 * [token] is minted **server-side** by `gen_random_uuid()`, not by the phone.
 * It is the one id in this schema the client must not choose: a client-minted
 * invite token is a token an attacker can also choose, and knowing it is the
 * whole of the authorisation to join.
 */
@Serializable
data class InviteRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("invited_by") val invitedBy: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
