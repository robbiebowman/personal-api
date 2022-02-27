package com.robbiebowman.personalapi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.robbiebowman.wordle.SolverEngine
import com.robbiebowman.wordle.Suggestion
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.InputStream

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.bind.annotation.PathVariable
import java.lang.NumberFormatException
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.http.HttpStatus

import org.springframework.web.server.ResponseStatusException

@RestController
class BibleleController {

    val numBibleLines: Long = 31102

    data class BibleVerse(val book: String, val chapter: Int, val verse: Int, val text: String)

    @GetMapping("/biblele/{id}")
    fun biblele(
        @PathVariable id: String
    ): BibleVerse {
        val idInt: Long
        try {
            idInt = Integer.parseInt(id).toLong()
            if (idInt < 0L || idInt >= numBibleLines) {
                throw NumberFormatException()
            }
        } catch (_: NumberFormatException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Can't interpret that id."
            )
        }
        var text: String
        Files.lines(Paths.get(ClassLoader.getSystemResource("static/bible_data_set.csv").toURI())).use { lines -> text = lines.skip(idInt).findFirst().get() }
        val verse = csvReader().readAll(text).first()
        return BibleVerse(verse[1], verse[2].toInt(), verse[3].toInt(), verse[4])
    }
}