package com.robbiebowman.personalapi

import com.robbiebowman.personalapi.auth.SlackAuthenticator.authenticate
import com.robbiebowman.personalapi.service.AsyncService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
class SummariseController {

    @Value("\${SLACK_TOKEN}")
    private val slackToken: String? = null

    @Value("\${OPEN_API_KEY}")
    private val openApiKey: String? = null

    @Value("\${SLACK_SIGNING_SECRET}")
    private val slackSigningSecret: String? = null

    private lateinit var asyncService: AsyncService;

    private lateinit var slackSummaryService: SlackSummaryService;

    @Autowired
    fun setAsyncService(asyncService: AsyncService) {
        this.asyncService = asyncService
    }

    @Autowired
    fun setSlackSummaryService(slackSummaryService: SlackSummaryService) {
        this.slackSummaryService = slackSummaryService
    }

    @PostMapping("/summarise", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @ResponseBody
    fun summarise(
        @RequestParam params: MultiValueMap<String, String>,
        httpRequest: HttpServletRequest,
        httpEntity: HttpEntity<String>,
        response: HttpServletResponse
    ) {
        // Authenticate request
        val timestamp = httpRequest.getHeader("X-Slack-Request-Timestamp").toLong()
        val signature = httpRequest.getHeader("X-Slack-Signature")
        authenticate(slackSigningSecret!!, signature, timestamp, httpEntity.body!!)

        // Get relevant form fields
        val channel = params["channel_id"]!!.first()
        val requestingUser = params["user_id"]!!.first()
        val arguments = params["text"]!!.firstOrNull() ?: "6 hours"
        val postPublicly = arguments.endsWith("publicly")
        val duration = parseHuman(arguments.replace("publicly", ""))

        response.status = HttpStatus.OK.value()
        response.contentType = "application/json"
        response.writer.write("""
            {
                "blocks": [
            		{
            			"type": "section",
            			"text": {
            				"type": "mrkdwn",
            				"text": "*Got it.*"
            			}
            		},
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": " Give me a few moments to read the messages in the last few hours and write a summary."
                        }
                    }
            	]
            }
        """.trimIndent())
        response.writer.flush()

        asyncService.process {
            slackSummaryService.postSummary(channel, requestingUser, duration, postPublicly)
        }
    }

    // Thank you Andreas
    // https://stackoverflow.com/a/52230282/1256019
    fun parseHuman(text: String): Duration {
        val m: Matcher = Pattern.compile(
            "\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" +
                    "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" +
                    "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" +
                    "\\s*", Pattern.CASE_INSENSITIVE
        )
            .matcher(text)
        if (!m.matches()) throw IllegalArgumentException("Not valid duration: $text")
        val hours = (if (m.start(1) == -1) 0 else m.group(1).toInt())
        val mins = (if (m.start(2) == -1) 0 else m.group(2).toInt())
        val secs = (if (m.start(3) == -1) 0 else m.group(3).toInt())
        return Duration.ofSeconds((hours * 60L + mins) * 60L + secs)
    }
}
