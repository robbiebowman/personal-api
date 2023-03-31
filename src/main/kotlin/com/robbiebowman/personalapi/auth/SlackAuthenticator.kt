package com.robbiebowman.personalapi.auth

import org.springframework.beans.factory.annotation.Value
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest
import kotlin.math.abs

object SlackAuthenticator {

    fun authenticate(slackSigningSecret: String, signature: String, timestamp: Long, rawBody: String) {
        // Validate timestamp
        val now = Instant.now().toEpochMilli()/1000
        if (abs(now - timestamp) > 60 * 5) throw Exception()

        // Validate signature
        val secretKeySpec = SecretKeySpec(slackSigningSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val toHash = "v0:$timestamp:$rawBody"
        val hash = mac.doFinal(toHash.toByteArray()).joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        val key = "v0=$hash"
        if (key != signature) throw Exception() // Invalid signature
    }
}
