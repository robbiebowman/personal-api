package com.robbiebowman.personalapi

import com.google.gson.Gson
import com.robbiebowman.*
import com.robbiebowman.personalapi.service.AsyncService
import com.robbiebowman.personalapi.service.BlobStorageService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*


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
        httpRequest: HttpServletRequest, response: HttpServletResponse
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

        val periodicTableQuestion = CompletePeriodicTableQuestion(
            definition = definition, description = periodicTableDescription
        )
        val id = UUID.randomUUID().toString()
        blobService.uploadToBlobStorage(
            containerName = containerName, blobName = id, thing = periodicTableQuestion
        )
        addToIndex(id, periodicTableQuestion)

        response.status = HttpStatus.OK.value()
        response.contentType = "application/json"
        response.writer.write(gson.toJson(periodicTableDescription))
        response.writer.flush()
    }

    private fun addToIndex(id: String, periodicTableQuestion: CompletePeriodicTableQuestion) {
        val existingIndex = blobService.getFromBlobStorage(
            containerName, "index", FileIndex::class.java
        )
        val newIndex = FileIndex(
            entries = existingIndex!!.entries + IndexEntry(
                id = id,
                type = getQuestionType(periodicTableQuestion),
                query = periodicTableQuestion.definition.query
            )
        )
        blobService.uploadToBlobStorage(containerName, "index", newIndex)
    }

    @GetMapping("/periodic-table/{id}")
    fun getPeriodicTable(
        @PathVariable id: String
    ): CompletePeriodicTableQuestion {
        val puzzle = blobService.getFromBlobStorage(
            containerName, id, CompletePeriodicTableQuestion::class.java
        )

        if (puzzle == null) {
            throw Exception("Puzzle doesn't exist!")
        }

        return puzzle
    }

    @GetMapping("/periodic-table")
    fun getIndex(): FileIndex {
        return blobService.getFromBlobStorage(
            containerName, "index", FileIndex::class.java
        )!!
    }

    @PostMapping("/periodic-table/refresh-index")
    fun refreshIndex(
    ) {
        val existingFileNames = blobService.getAllFileNamesInContainer(containerName).filter {
            try {
                UUID.fromString(it)
                true
            } catch (e: Exception) {
                false
            }
        }

        val existingFiles = existingFileNames.mapNotNull { id ->
            blobService.getFromBlobStorage(
                containerName, id, CompletePeriodicTableQuestion::class.java
            )?.let { id to it }
        }.map { (id, definition) ->
            val type = getQuestionType(definition)
            IndexEntry(
                id = id,
                type = type,
                query = definition.definition.query,
            )
        }

        blobService.uploadToBlobStorage(containerName, "index", FileIndex(existingFiles))
    }

    private fun getQuestionType(definition: CompletePeriodicTableQuestion) =
        if (definition.definition.categories.isNotEmpty()) {
            QuestionType.Category
        } else if (definition.definition.rangeMax != null) {
            QuestionType.Range
        } else {
            QuestionType.OpenQuestion
        }

}

data class CategoryDefinition(
    val name: String, val hexColour: String
)

data class RangeDefinition(
    val value: Int, val hexColour: String
)

data class TableDefinition(
    val query: String,
    val rangeMin: RangeDefinition?,
    val rangeMax: RangeDefinition?,
    val categories: List<CategoryDefinition>,
)

data class CompletePeriodicTableQuestion(
    val definition: TableDefinition, val description: ElementDescriber.PeriodicTableDescription
)

data class FileIndex(
    val entries: List<IndexEntry>
)

data class IndexEntry(
    val id: String, val type: QuestionType, val query: String
)

enum class QuestionType {
    OpenQuestion, Range, Category
}