package com.robbiebowman.personalapi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.robbiebowman.wordle.SolverEngine
import com.robbiebowman.wordle.Suggestion
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.bind.annotation.PathVariable
import java.lang.NumberFormatException
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.http.HttpStatus

import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.nio.file.Path
import kotlin.random.Random
import java.util.Collections

import java.nio.file.FileSystems

import org.springframework.util.ResourceUtils
import java.io.*


@RestController
class ChessEvalController {

    val numChessEvalLines = 100_000

    enum class Difficulties {
        Easy, Medium, Hard
    }

    data class ChessEvaluation(val fen: String, val evaluation: String)

    @GetMapping("/chess-evals")
    fun getRandomChessEvaluations(
        @RequestParam("difficulty") difficulty: Difficulties? = Difficulties.Medium
    ): ChessEvaluation {
        val lineNum = Random.nextInt(numChessEvalLines)
        var text: String
        val filePath = when(difficulty) {
            Difficulties.Easy -> "static/chess/easy-puzzles.csv"
            Difficulties.Medium, null -> "static/chess/medium-puzzles.csv"
            Difficulties.Hard -> "static/chess/hard-puzzles.csv"
        }
        val reader = BufferedReader(InputStreamReader(this.javaClass.classLoader.getResourceAsStream(filePath)!!))
        for (i in (1..lineNum)) {
            reader.readLine()
        }
        val line = csvReader().readAll(reader.readLine()).first()
        return ChessEvaluation(line[0], line[1])
    }
}