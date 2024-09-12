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

    @PostMapping("/periodic-table", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    suspend fun askPeriodicTable(
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val requestBody = httpRequest.reader.readText()
        val definition = gson.fromJson(requestBody, TableDefinition::class.java)

        val periodicTableDescription = if (definition.categories.isEmpty() && definition.rangeMin == null) {
            describer.value.askOpenQuestionOfElements(definition.query)
        } else if (definition.categories.isEmpty()) {
            describer.value.rateElements(definition.query, definition.rangeMin!!.value, definition.rangeMax!!.value)
        } else {
            describer.value.categoriseElements(definition.query, definition.categories.map { it.name })
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
                    definition = definition,
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

data class CategoryDefinition(
    val name: String,
    val hexColour: String
)

data class RangeDefinition(
    val value: Int,
    val hexColour: String
)

data class TableDefinition(
    val query: String,
    val rangeMin: RangeDefinition?,
    val rangeMax: RangeDefinition?,
    val categories: List<CategoryDefinition>,
)

data class CompletePeriodicTableQuestion(
    val definition: TableDefinition,
    val description: ElementDescriber.PeriodicTableDescription
)