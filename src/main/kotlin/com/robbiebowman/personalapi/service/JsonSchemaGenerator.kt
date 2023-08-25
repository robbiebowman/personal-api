package com.robbiebowman.personalapi.service

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.JsonNode
import com.github.victools.jsonschema.generator.*
import com.github.victools.jsonschema.module.jackson.JacksonModule


class JsonSchemaGenerator {

    fun getWeatherSchema() {
        val json = createJsonSchema<Weather>()
        println(json)
    }

    inline fun <reified T> createJsonSchema(): String {
        val module = JacksonModule()
        val configBuilder = SchemaGeneratorConfigBuilder(
            SchemaVersion.DRAFT_2020_12,
            OptionPreset.PLAIN_JSON,
        ).with(module)
        val config: SchemaGeneratorConfig = configBuilder.build()
        val generator = SchemaGenerator(config)
        val jsonSchema: JsonNode = generator.generateSchema(T::class.java)

        return jsonSchema.toPrettyString()
    }

}

data class Weather(
    @get:JsonPropertyDescription("Method Title")
    val longitude: Double,
    @get:JsonPropertyDescription("Method Title2")
    val latitude: Double,
    @get:JsonPropertyDescription("Method Title3")
    val cityName: String?,
    @get:JsonPropertyDescription("Method Title3")
    val feelsLike: Feeling
)

enum class Feeling {
    Warm, Cold, Mild
}