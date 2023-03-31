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

    fun authenticate(slackSigningSecret: String, httpRequest: HttpServletRequest, rawBody: String) {
        val secretKeySpec = SecretKeySpec(slackSigningSecret.toByteArray(), "HmacSHA256")
        val mac: Mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)

        val timestamp = httpRequest.getHeader("X-Slack-Request-Timestamp").toLong()
        val signature = httpRequest.getHeader("X-Slack-Signature")
        val now = Instant.now().toEpochMilli()
        println("About to check time")
        if (abs(now - timestamp) > 60 * 5) throw Exception() // Replay attack or clock desync
        println("Time good")

        val body = String(httpRequest.inputStream.readAllBytes(), StandardCharsets.UTF_8)
        val toHash = "v0:$timestamp:$body"
        val hash = mac.doFinal(toHash.toByteArray()).joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        val key = "v0=$hash"

        println("key: $key")
        println("sig: $signature")
        if (key != signature) throw Exception() // Invalid signature
    }

}