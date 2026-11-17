package com.menu.app.domain.auth

import java.io.IOException
import org.json.JSONException

/** Translate expected transport/response failures without swallowing coroutine cancellation. */
internal inline fun authRequest(block: () -> AuthResult): AuthResult = try {
    block()
} catch (error: IOException) {
    AuthResult.Failure("Unable to connect. Check your connection and try again.")
} catch (error: JSONException) {
    AuthResult.Failure("The server returned an invalid response. Please try again.")
}