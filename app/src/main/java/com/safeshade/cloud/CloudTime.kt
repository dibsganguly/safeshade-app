package com.safeshade.cloud

import java.time.Instant
import java.time.OffsetDateTime

/**
 * How the app reads a timestamp the server wrote.
 *
 * ### `Instant.parse` is not enough, and the way it fails is silent
 *
 * PostgREST renders `timestamptz` as `2026-09-07T10:00:00.123456+00:00`, with a
 * numeric offset - not the `Z` that `Instant.toString()` produces. `Instant`
 * only learned to parse an offset in JDK 12 (JDK-8166138), and Android's
 * `java.time` on the API levels this app supports predates that. So on a phone,
 * `Instant.parse` throws on the ordinary shape of every timestamp the server
 * sends, while on a desktop JVM - where the unit tests run, and where
 * `FakeCloudClient` stamps a `Z` anyway - it works perfectly.
 *
 * Nothing about that failure is loud. The three places it lands are:
 *
 *  * the pull cursor, which becomes null, so every drain asks for the whole
 *    table again, forever, on cellular data;
 *  * a pulled alert's `occurred_at`, which falls back to zero, so a fall from
 *    this morning sorts to 1970 in the trip log;
 *  * an invitation's `expires_at`, which never compares as past, so a dead
 *    invitation shows as Pending until somebody sends another one.
 *
 * `OffsetDateTime.parse` accepts both spellings on every API level this app
 * runs on, which is the whole of the fix.
 *
 * ### It returns null rather than throwing
 *
 * Every caller is inside a pull or a merge, and a parse failure there must not
 * take down a drain that is also carrying a fall alert. Null means "this
 * timestamp is unreadable", and each caller already has a sane answer for that.
 */
internal fun parseServerInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(raw) }.getOrNull()
}
