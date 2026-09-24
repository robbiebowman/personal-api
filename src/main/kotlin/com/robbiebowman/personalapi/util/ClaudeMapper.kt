package com.robbiebowman.personalapi.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.robbiebowman.claude.json.IgnoreRequiredFieldFilter

fun claudeMapper(): ObjectMapper = jacksonObjectMapper()
    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    // Claude Sonnet 5 may return thinking blocks, which this older SDK does not model.
    .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
    .addMixIn(JsonSchema::class.java, IgnoreRequiredFieldFilter::class.java)
    .setFilterProvider(IgnoreRequiredFieldFilter.provider)
