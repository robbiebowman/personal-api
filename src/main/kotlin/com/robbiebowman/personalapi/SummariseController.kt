package com.robbiebowman.personalapi

import com.robbiebowman.personalapi.auth.SlackAuthenticator.authenticate
import com.robbiebowman.personalapi.service.AsyncService
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.model.Message
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.Instant
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
            slackSummaryService.postSummary(channel, requestingUser)
        }
    }
}
