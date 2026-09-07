package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.FirmwareReleaseRow
import kotlinx.serialization.json.Json

/**
 * Rows off the wire, decoded leniently.
 *
 * `ignoreUnknownKeys` because a migration that adds a column must not make an
 * unupdated phone throw on the read that would have told it to update.
 */
private val firmwareJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

/**
 * What firmware is published, and where the image is.
 *
 * ### The one part of the cloud that works signed out
 *
 * `firmware_releases` carries `for select using (true)` and the `firmware`
 * bucket is public, both on purpose: a device asking whether it is out of date
 * should not need an account. So [releases] is readable by a guest session and
 * [downloadUrl] mints no signed URL and takes no session - it is a string.
 *
 * That is the opposite of every other bucket in this app, and the reason is
 * worth being explicit about rather than treating as a quirk: the images are
 * signed, or they are not trustworthy whether or not the list is private. There
 * is nothing in a release row anybody could misuse, and there is a real cost to
 * making an update check depend on being logged in.
 *
 * ### What this class does not do
 *
 * It does not flash anything, does not download bytes, and does not decide
 * whether an update is wanted. It answers two questions - what exists, and
 * where one lives - and the decision belongs to whatever screen is asking. In
 * particular nothing here compares [FirmwareReleaseRow.versionCode] against the
 * device's own version: the device reports that over BLE, and this layer has
 * never spoken to a device.
 */
class FirmwareCloud(private val client: CloudClient) {

    /**
     * Every published release for one model, newest first.
     *
     * Ordered by `version_code` and never by [FirmwareReleaseRow.version],
     * which is a display string. That is the entire reason the column exists:
     * "1.10.0" sorts below "1.9.0" as text, so a phone sorting by name would be
     * offered an *older* image than the one it is running and would call it an
     * update.
     *
     * @param model `s1`, `5g` or `spark` - the values the table's check
     *   constraint allows. Anything else is not an error here; it simply
     *   matches no rows, which is what a model this server has never published
     *   for honestly looks like.
     */
    suspend fun releases(model: String): CloudResult<List<FirmwareReleaseRow>> {
        val wanted = model.trim()
        if (wanted.isBlank()) return CloudResult.Ok(emptyList())

        val rows = when (
            val result = client.selectWhereIn(
                table = CloudTables.FIRMWARE_RELEASES,
                column = "model",
                values = listOf(wanted),
                orderBy = "version_code",
                descending = true
            )
        ) {
            is CloudResult.Ok -> result.value
            is CloudResult.Failed -> return result
            CloudResult.Disabled -> return CloudResult.Disabled
        }

        val decoded = rows.mapNotNull { row ->
            runCatching { firmwareJson.decodeFromJsonElement(FirmwareReleaseRow.serializer(), row) }
                .getOrNull()
        }
        return CloudResult.Ok(usable(decoded))
    }

    /**
     * The public URL of a release image, or null when there is none to give.
     *
     * Null for two different situations that are the same answer: this build
     * has no project configured, or the row carried no `storage_path`. Neither
     * is an error worth a [CloudResult] - a caller with no URL simply cannot
     * offer the download, and there is nothing for a person to do about either.
     *
     * Nothing is asked of the server. A public object's URL is composed from
     * the project URL and the path, which is why this does not suspend and
     * cannot report whether the image is actually there.
     */
    fun downloadUrl(storagePath: String): String? {
        val path = storagePath.trim().trimStart('/')
        if (path.isBlank()) return null
        return client.publicUrl(CloudTables.Buckets.FIRMWARE, path)
    }

    companion object {

        /**
         * The rows a phone could actually act on, newest first.
         *
         * Pure and internal so it can be tested without a client. Three rules,
         * each of which describes a row that would otherwise be offered as an
         * update and then fail at the moment somebody tapped it:
         *
         *  - a soft-deleted release is withdrawn, and a withdrawn image is
         *    usually withdrawn *because* it bricked something;
         *  - a row with no `storage_path` names no file to fetch;
         *  - a row with no `version_code` cannot be compared to anything, so it
         *    can neither be established as newer nor sorted into place.
         *
         * The sort is repeated here rather than trusted from the server, for
         * the reason [SightingsCloud.newestPerAddress] repeats its own: an
         * ordering assumed and quietly lost fails by offering the wrong image,
         * which is the one failure mode of this feature that matters.
         */
        internal fun usable(rows: List<FirmwareReleaseRow>): List<FirmwareReleaseRow> =
            rows.filter { row ->
                row.deletedAt.isNullOrBlank() &&
                    !row.storagePath.isNullOrBlank() &&
                    row.versionCode != null
            }.sortedByDescending { it.versionCode ?: Int.MIN_VALUE }
    }
}
