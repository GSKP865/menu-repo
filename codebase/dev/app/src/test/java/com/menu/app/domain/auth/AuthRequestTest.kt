package com.menu.app.domain.auth

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRequestTest {
    @Test fun `offline requests return a recoverable failure`() {
        assertTrue(authRequest { throw IOException("offline") } is AuthResult.Failure)
    }

    @Test fun `timeouts return a recoverable failure`() {
        assertTrue(authRequest { throw SocketTimeoutException("timeout") } is AuthResult.Failure)
    }

    @Test fun `successful results are preserved`() {
        val result = AuthResult.Success()
        assertEquals(result, authRequest { result })
    }

    @Test(expected = CancellationException::class)
    fun `cancellation still cancels the operation`() {
        authRequest { throw CancellationException("screen closed") }
    }
}