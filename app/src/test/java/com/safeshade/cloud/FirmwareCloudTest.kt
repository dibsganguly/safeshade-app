package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.FirmwareReleaseRow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What firmware is published, and where the image lives.
 *
 * The failure this file exists to prevent is specific and quiet: a phone
 * offered an *older* image than the one it is running, because the list was
 * sorted by the display version rather than by `version_code`. "1.10.0" sorts
 * below "1.9.0" as text. That is why the column exists at all, and why the
 * ordering is asserted rather than assumed.
 */
class FirmwareCloudTest {

    private fun release(
        id: String,
        versionCode: Int?,
        version: String,
        storagePath: String? = "s1/$version.bin",
        deletedAt: String? = null
    ) = FirmwareReleaseRow(
        id = id,
        model = "s1",
        version = version,
        versionCode = versionCode,
        storagePath = storagePath,
        byteSize = 1_048_576L,
        mandatory = false,
        deletedAt = deletedAt
    )

    // ============================================
    // THE URL
    // ============================================

    @Test
    fun `a public bucket url is the project url, the bucket and the path`() {
        val url = FirmwareCloud(FakeCloudClient()).downloadUrl("s1/1.10.0.bin")
        assertEquals("https://fake.local/public/firmware/s1/1.10.0.bin", url)
    }

    @Test
    fun `a leading slash on the path does not double up in the url`() {
        val url = FirmwareCloud(FakeCloudClient()).downloadUrl("/s1/1.10.0.bin")
        assertEquals("https://fake.local/public/firmware/s1/1.10.0.bin", url)
    }

    /**
     * A build with no project has no URL to give. Null rather than a plausible
     * string, so nothing downstream can offer a download that would 404.
     */
    @Test
    fun `a build with no cloud has no url rather than a wrong one`() {
        assertNull(FirmwareCloud(FakeCloudClient(disabled = true)).downloadUrl("s1/1.0.0.bin"))
    }

    @Test
    fun `a row with no storage path names no file, so there is no url`() {
        assertNull(FirmwareCloud(FakeCloudClient()).downloadUrl("   "))
    }

    // ============================================
    // THE LIST
    // ============================================

    @Test
    fun `releases come back newest first, by version code and never by name`() = runBlocking {
        val client = FakeCloudClient()
        client.upsert(
            CloudTables.FIRMWARE_RELEASES,
            listOf(
                release("a", versionCode = 9, version = "1.9.0"),
                release("b", versionCode = 10, version = "1.10.0"),
                release("c", versionCode = 2, version = "1.2.0")
            ),
            FirmwareReleaseRow.serializer()
        )

        val result = FirmwareCloud(client).releases("s1")
        assertTrue(result is CloudResult.Ok)
        val rows = (result as CloudResult.Ok).value
        // "1.10.0" sorts below "1.9.0" as text. This is the assertion that says
        // the phone is offered the newer image and not the older one.
        assertEquals(listOf("1.10.0", "1.9.0", "1.2.0"), rows.map { it.version })
    }

    @Test
    fun `another model's releases are not offered`() = runBlocking {
        val client = FakeCloudClient()
        client.upsert(
            CloudTables.FIRMWARE_RELEASES,
            listOf(
                release("a", versionCode = 3, version = "1.3.0"),
                release("b", versionCode = 4, version = "2.0.0").copy(model = "spark")
            ),
            FirmwareReleaseRow.serializer()
        )

        val rows = (FirmwareCloud(client).releases("s1") as CloudResult.Ok).value
        assertEquals(listOf("1.3.0"), rows.map { it.version })
    }

    @Test
    fun `a model this server has never published for is empty, not an error`() = runBlocking {
        val result = FirmwareCloud(FakeCloudClient()).releases("5g")
        assertEquals(CloudResult.Ok(emptyList<FirmwareReleaseRow>()), result)
    }

    @Test
    fun `a blank model asks nothing of the server`() = runBlocking {
        val client = FakeCloudClient().apply { failNext = "should not be consumed" }
        assertEquals(CloudResult.Ok(emptyList<FirmwareReleaseRow>()), FirmwareCloud(client).releases(" "))
        assertEquals("should not be consumed", client.failNext)
    }

    @Test
    fun `a build with no cloud says so rather than reporting no firmware`() = runBlocking {
        assertEquals(CloudResult.Disabled, FirmwareCloud(FakeCloudClient(disabled = true)).releases("s1"))
    }

    /**
     * Each of these rows would be offered as an update and then fail at the
     * moment somebody tapped it. A withdrawn release in particular is usually
     * withdrawn *because* it bricked something.
     */
    @Test
    fun `a withdrawn, pathless or uncomparable release is not offered`() {
        val usable = FirmwareCloud.usable(
            listOf(
                release("a", versionCode = 5, version = "1.5.0"),
                release("b", versionCode = 6, version = "1.6.0", deletedAt = "2026-09-01T00:00:00Z"),
                release("c", versionCode = 7, version = "1.7.0", storagePath = null),
                release("d", versionCode = null, version = "1.8.0")
            )
        )
        assertEquals(listOf("1.5.0"), usable.map { it.version })
    }
}
