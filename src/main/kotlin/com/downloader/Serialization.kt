package com.downloader

import com.fasterxml.jackson.annotation.JsonInclude.Include.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.SerializationFeature.*
import com.fasterxml.jackson.module.kotlin.*
import java.text.*

fun ObjectMapper.setup() = run {

    registerModule(KotlinModule())
    enable(INDENT_OUTPUT)
    setSerializationInclusion(NON_NULL)
    dateFormat = SimpleDateFormat("yyyy-MM-dd")

    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    this
}

/**
 * Объект для обработки JSON.
 */
val jsonMapper = jacksonObjectMapper().setup()

/**
 * Сериализует объект в JSON.
 *
 * @return JSON.
 */
fun Any.toJson() = jsonMapper.writeValueAsString(this)!!

/**
 * Преобразует JSON в объект типа [T].
 *
 * @param T тип для преобразования.
 */
inline fun <reified T> String.parseJson() = jsonMapper.readValue<T>(this)
