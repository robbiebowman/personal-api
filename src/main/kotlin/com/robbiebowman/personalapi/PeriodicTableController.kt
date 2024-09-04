package com.robbiebowman.personalapi

import com.google.gson.Gson
import com.robbiebowman.*
import com.robbiebowman.claude.ClaudeClientBuilder
import com.robbiebowman.claude.MessageContent
import com.robbiebowman.claude.Role
import com.robbiebowman.claude.SerializableMessage
import com.robbiebowman.personalapi.service.AsyncService
import com.robbiebowman.personalapi.service.BlobStorageService
import com.robbiebowman.personalapi.util.DateUtils
import com.robbiebowman.personalapi.util.DateUtils.getCurrentDateDirectoryName
import com.robbiebowman.personalapi.util.DateUtils.isWithinAcceptableDateRange
import com.robbiebowman.personalapi.util.HumanIdGenerator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.NoSuchElementException


@RestController
class PeriodicTableController {

    @Autowired
    private lateinit var blobService: BlobStorageService

    private val containerName = "periodic-table"

    @Value("\${claude_api_key}")
    private val claudeApiKey: String? = null

    private val gson = Gson()

    private val describer = lazy { ElementDescriber(claudeApiKey!!) }

    private lateinit var asyncService: AsyncService

    @Autowired
    fun setAsyncService(asyncService: AsyncService) {
        this.asyncService = asyncService
    }

    @PostMapping("/periodic-table", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @ResponseBody
    suspend fun askPeriodicTable(
        @RequestParam params: MultiValueMap<String, String>,
        httpRequest: HttpServletRequest,
        httpEntity: HttpEntity<String>,
        response: HttpServletResponse
    ) {
        val query = params["query"]!!.single()
        val rangeMin = params["rangeMin"]?.singleOrNull()?.toInt()
        val rangeMax = params["rangeMax"]?.singleOrNull()?.toInt()
        val categories = params["categories"] ?: emptyList()

        val periodicTableDescription = if (categories.isEmpty() && rangeMin == null) {
            describer.value.askOpenQuestionOfElements(query)
        } else if (categories.isEmpty()) {
            describer.value.rateElements(query, rangeMin!!, rangeMax!!)
        } else {
            describer.value.categoriseElements(query, categories)
        }


        response.status = HttpStatus.OK.value()
        response.contentType = "application/json"
        response.writer.write(gson.toJson(periodicTableDescription))
        response.writer.flush()

        asyncService.process {
            blobService.uploadToBlobStorage(
                containerName = containerName,
                blobName = UUID.randomUUID().toString(),
                thing = CompletePeriodicTableQuestion(
                    query = query,
                    rangeMin = rangeMin,
                    rangeMax = rangeMax,
                    categories = categories,
                    description = periodicTableDescription
                )
            )
        }
    }

    @GetMapping("/periodic-table")
    fun getPeriodicTable(
        @RequestParam(value = "id") id: String? = null,
    ): CompletePeriodicTableQuestion {
        val puzzle = blobService.getFromBlobStorage(
            containerName,
            id!!,
            CompletePeriodicTableQuestion::class.java
        )

        if (puzzle == null) {
            throw Exception("Puzzle doesn't exist!")
        }

        return puzzle
    }

}

data class CompletePeriodicTableQuestion(
    val query: String,
    val rangeMin: Int?,
    val rangeMax: Int?,
    val categories: List<String>,
    val description: ElementDescriber.PeriodicTableDescription
)