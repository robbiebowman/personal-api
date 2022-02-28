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
import kotlin.random.Random

@RestController
class ChessEvalController {

    val numChessEvalLines = 100_000

    enum class Difficulties {
        Easy, Medium, Hard
    }

    data class ChessEvaluation(val fen: String, val evaluation: String)

    @GetMapping("/chess-evals")
    fun biblele(
        @RequestParam("difficulty") difficulty: Difficulties? = Difficulties.Medium
    ): ChessEvaluation {
        val lineNum = Random.nextInt(numChessEvalLines)
        var text: String
        val file = when(difficulty) {
            Difficulties.Easy -> "static/chess/easy-puzzles.csv"
            Difficulties.Medium, null -> "static/chess/medium-puzzles.csv"
            Difficulties.Hard -> "static/chess/hard-puzzles.csv"
        }
        Files.lines(Paths.get(ClassLoader.getSystemResource(file).toURI()))
            .use { lines -> text = lines.skip(lineNum.toLong()).findFirst().get() }
        val line = csvReader().readAll(text).first()
        return ChessEvaluation(line[0], line[1])
    }
}