package com.safeshade.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * The one piece of JSON handling in `cloud/` that is worth testing on its own.
 *
 * `SupabaseCloudClient` cannot be exercised in a unit test — it needs a real
 * project, a real socket and an Android `Intent` on its interface — so anything
 * inside it that can be wrong quietly has to be lifted out to where a test can
 * reach it. This file is that place, and today it holds exactly one function.
 */

/** Shared with the client's own configuration; see `SupabaseCloudClient.json`. */
private val rpcJson = Json { ignoreUnknownKeys = true }

/**
 * Turns PostgREST's raw RPC response body into a [JsonElement].
 *
 * ### The empty-body case is the whole reason this exists
 *
 * A Postgres function declared `returns void` answers with a **zero-length
 * body**, not with `null` and not with `{}`. `Json.parseToJsonElement("")`
 * throws on that, so the naive `parseToJsonElement(result.data)` turns every
 * successful call to a void function into a `CloudResult.Failed` carrying a
 * parser message — a call that did exactly what it was asked to do, reported to
 * the user as an error, and retried forever by the outbox because a parse
 * failure looks transient.
 *
 * So a blank body is [JsonNull]: the function ran and had nothing to say.
 *
 * A body that is present but not JSON is **not** swallowed. That would be a
 * PostgREST contract violation, and quietly mapping it to null would hide a
 * real fault behind a value that reads as "no result". It throws, and
 * `SupabaseCloudClient.runOrFail` turns it into a plain-English failure like
 * every other fault.
 */
internal fun parseRpcData(raw: String): JsonElement =
    if (raw.isBlank()) JsonNull else rpcJson.parseToJsonElement(raw)
