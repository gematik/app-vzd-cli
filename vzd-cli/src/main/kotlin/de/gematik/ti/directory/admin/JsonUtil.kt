package de.gematik.ti.directory.admin

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.Reader
import java.io.StringWriter
import java.math.BigDecimal

fun jsonArraySequence(input: Reader): Sequence<String> = sequence {
    val reader = JsonReader(input)

    reader.beginArray()
    while (reader.hasNext()) {
        val strWriter = StringWriter()
        val writer = JsonWriter(strWriter)
        reader.beginObject()
        writer.beginObject()
        var depth = 0
        while (reader.peek() != JsonToken.END_OBJECT || depth > 0) {
            val token = reader.peek()
            when (token) {
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    writer.beginObject()
                    depth++
                }
                JsonToken.END_OBJECT -> {
                    reader.endObject()
                    writer.endObject()
                    depth--
                }
                JsonToken.NAME -> writer.name(reader.nextName())
                JsonToken.BOOLEAN -> writer.value(reader.nextBoolean())
                JsonToken.STRING -> writer.value(reader.nextString())
                JsonToken.NULL -> {
                    reader.nextNull()
                    writer.nullValue()
                }
                JsonToken.NUMBER -> writer.value(BigDecimal(reader.nextString()))
                JsonToken.BEGIN_ARRAY -> {
                    reader.beginArray()
                    writer.beginArray()
                }
                JsonToken.END_ARRAY -> {
                    reader.endArray()
                    writer.endArray()
                }
                else -> reader.skipValue()
            }
        }

        reader.endObject()
        writer.endObject()
        yield(strWriter.toString())
    }
    reader.endArray()
}
