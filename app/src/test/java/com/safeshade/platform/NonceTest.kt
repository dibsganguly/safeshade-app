package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [sha256Hex] against a known test vector. This is the one line in
 * [GoogleSignInHelper] where getting the hashing backwards (or using the
 * wrong digest) would look fine in manual testing and only break Supabase's
 * server-side nonce comparison — worth pinning to a fixed answer.
 */
class NonceTest {

    @Test
    fun `sha256 of abc matches the known vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc"),
        )
    }

    @Test
    fun `sha256 of empty string matches the known vector`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex(""),
        )
    }
}
