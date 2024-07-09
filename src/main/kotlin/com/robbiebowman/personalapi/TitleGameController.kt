package com.robbiebowman.personalapi

import com.robbiebowman.com.robbiebowman.BlurbAndInfo
import com.robbiebowman.com.robbiebowman.PretendFilmGenerator
import com.robbiebowman.personalapi.service.BlobStorageService
import com.robbiebowman.personalapi.util.DateUtils.getCurrentDateDirectoryName
import com.robbiebowman.personalapi.util.DateUtils.isWithinAcceptableDateRange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate


@RestController
class TitleGameController {

    @Value("\${open_ai_api_key}")
    private val openApiKey: String? = null

    @Value("\${claude_api_key}")
    private val claudeApiKey: String? = null

    private val containerName: String = "title-games"

    @Autowired
    private lateinit var blobService: BlobStorageService

    @GetMapping("/title-game")
    fun getGame(
        @RequestParam(value = "date") date: LocalDate? = null,
        @RequestParam(value = "id") id: String? = null,
    ): BlurbAndInfo? {
        val dir = if (date != null) {
            if (!isWithinAcceptableDateRange(date)) throw Exception("Invalid date")
            getCurrentDateDirectoryName(date)
        } else if (id != null) {
            "custom/$id"
        } else {
            throw Exception("Puzzle doesn't exist!")
        }
        val fileName = "${dir}/film-info-and-blurb.json"

        val puzzle = blobService.getFromBlobStorage(containerName, fileName, BlurbAndInfo::class.java)

        return puzzle
    }


    @GetMapping("/title-game/create")
    fun createMiniCrossword(
        @RequestParam(value = "date") date: LocalDate = LocalDate.now(),
    ): BlurbAndInfo {
        if (!isWithinAcceptableDateRange(date)) throw Exception("Invalid date")
        val dir = getCurrentDateDirectoryName(date)
        val fileName = "${dir}/film-info-and-blurb.json"

        val generator = PretendFilmGenerator(claudeApiKey!!, openApiKey!!)

        val puzzle = generator.generatePretendFilm()

        blobService.uploadToBlobStorage(containerName, fileName, puzzle)

        return puzzle
    }

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND, code = HttpStatus.NOT_FOUND)
    fun handleNoSuchElementFoundException(
        exception: NoSuchElementException
    ): ResponseEntity<String?>? {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.message)
    }
}