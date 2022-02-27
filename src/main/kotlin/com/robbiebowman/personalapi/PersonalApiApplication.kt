package com.robbiebowman.personalapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RequestParam

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@RestController
class PersonalApiApplication {
	@GetMapping("/hello")
	fun hello(@RequestParam(value = "name", defaultValue = "World") name: String?): String? {
		return String.format("Hello %s!", name)
	}
}

fun main(args: Array<String>) {
	runApplication<PersonalApiApplication>(*args)
}
