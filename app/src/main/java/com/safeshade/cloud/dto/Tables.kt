package com.safeshade.cloud.dto

/**
 * Every table name, once.
 *
 * These strings appear in three unrelated places — the PostgREST calls here,
 * the outbox entries persisted on disk, and `supabase/migrations/0001_init.sql`
 * — and a typo in any of them is invisible until a write silently 404s at
 * runtime. Constants at least make the app-side two agree with each other, and
 * a rename becomes one edit plus a SQL migration rather than a grep.
 */
object CloudTables {
    const val PROFILES = "profiles"
    const val CIRCLES = "circles"
    const val CIRCLE_MEMBERS = "circle_members"
    const val WEARERS = "wearers"
    const val DEVICES = "devices"
    const val MEDICAL_IDS = "medical_ids"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val ALERTS = "alerts"
    const val ALERT_DELIVERIES = "alert_deliveries"
    const val MESSAGES = "messages"
    const val VITALS_SAMPLES = "vitals_samples"
    const val EVIDENCE = "evidence"
    const val ZONES = "zones"
    const val ZONE_EVENTS = "zone_events"
    const val SUBSCRIPTIONS = "subscriptions"
    const val INVITES = "invites"
    const val FIRMWARE_RELEASES = "firmware_releases"
    const val SMART_HOME_HOOKS = "smart_home_hooks"
    const val DEVICE_SIGHTINGS = "device_sightings"

    /** Storage buckets. Created by the same migration. */
    object Buckets {
        /** Private. Voice and photo evidence from a fall or an SOS. */
        const val EVIDENCE = "evidence"

        /** Public. OTA firmware images; readable without a session by design. */
        const val FIRMWARE = "firmware"

        /** Private. The 30-second surroundings recordings. */
        const val VOICE = "voice"

        /** Public. Circle-member avatars. */
        const val AVATARS = "avatars"
    }
}
