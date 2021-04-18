package dev.brella.blasement.client.jvm

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigInteger
import java.security.MessageDigest

sealed class JsonSchema {
    abstract fun toString(builder: StringBuilder, indent: Int): StringBuilder
    data class JsonObjectSchema(val elements: Map<String, JsonSchema>) : JsonSchema() {
        override fun toString(builder: StringBuilder, indent: Int) = builder.apply {
            appendLine("json {")

            elements.forEach { (key, value) ->
                repeat(indent + 1) { append('\t') }
                append(key)
                append(": ")
                value.toString(builder, indent + 1)
            }

            repeat(indent) { append('\t') }
            appendLine("}")
        }
    }

    data class JsonArraySchema(val elements: List<JsonSchema>) : JsonSchema() {
        override fun toString(builder: StringBuilder, indent: Int): StringBuilder = builder.apply {
            appendLine("json [")

            elements.forEach { value ->
                repeat(indent + 1) { append('\t') }
                value.toString(builder, indent + 1)
            }

            repeat(indent) { append('\t') }
            appendLine("]")
        }
    }

    object JsonString : JsonSchema() {
        override fun toString(builder: StringBuilder, indent: Int): StringBuilder =
            builder.appendLine("string")
    }

    sealed class JsonNumber : JsonSchema() {
        object Whole : JsonNumber() {
            override fun toString(builder: StringBuilder, indent: Int): StringBuilder =
                builder.appendLine("int")
        }

        object Decimal : JsonNumber() {
            override fun toString(builder: StringBuilder, indent: Int): StringBuilder =
                builder.appendLine("double")
        }
    }

    object JsonBoolean : JsonSchema() {
        override fun toString(builder: StringBuilder, indent: Int): StringBuilder =
            builder.appendLine("boolean")
    }

    object JsonNull : JsonSchema() {
        override fun toString(builder: StringBuilder, indent: Int): StringBuilder =
            builder.appendLine("null")
    }

    val hash: String by lazy {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(buildString { toString(this, 0) }.encodeToByteArray())
        String.format("%0${hashBytes.size shl 1}x", BigInteger(1, hashBytes))
    }
}

fun JsonElement.buildSchema(): JsonSchema =
    when (this) {
        is JsonObject -> JsonSchema.JsonObjectSchema(mapValues { (_, value) -> value.buildSchema() })
        is JsonArray -> JsonSchema.JsonArraySchema(map { it.buildSchema() })
        is JsonNull -> JsonSchema.JsonNull
        is JsonPrimitive -> {
            when {
                isString -> JsonSchema.JsonString
                content.toBooleanStrictOrNull() != null -> JsonSchema.JsonBoolean
                content.toLongOrNull() != null -> JsonSchema.JsonNumber.Whole
                content.toDoubleOrNull() != null -> JsonSchema.JsonNumber.Decimal
                else -> throw IllegalStateException("What on earth is $content ???")
            }
        }
    }

internal fun String.toBooleanStrictOrNull(): Boolean? = when {
    this.equals("true", ignoreCase = true) -> true
    this.equals("false", ignoreCase = true) -> false
    else -> null
}