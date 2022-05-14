package vzd.teststuite.admin

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import vzd.admin.client.Client
import java.io.InputStreamReader
import java.io.StringWriter
import java.math.BigDecimal

class TestDump : FeatureSpec({
    var client: Client? = null

    beforeSpec {
        client = createClient()
    }

    feature("Herunteladen von großen Mengen von Einträgen als Stream") {
        scenario("Abfrage nach holder liefert alle Einträge des Kartenherausgebers") {
            client?.streamDirectoryEntries(mapOf("holder" to "gematik_test")) {
                println(it.directoryEntryBase)
            }
        }

        scenario("Parse JSON Array") {
            val jsonString = """
                [
                    { "a": 1, "b": "foo", "c":"{", "d": [ 1, 2, 3, 4 ] },
                    { "a": 2, "b": "bar", "c":"}}" },
                    { "a": 3, "b": "zoo", "c":"\"{" }
                ]
            """.trimIndent()

            val json = Json.parseToJsonElement(jsonString) as JsonArray
            json.size shouldBe 3

            val input = jsonString.byteInputStream(Charsets.UTF_8)

            val reader = JsonReader(InputStreamReader(input))

            val sequence = sequence {
                reader.beginArray()
                while (reader.hasNext()) {
                    val strWriter = StringWriter()
                    val writer = JsonWriter(strWriter)
                    reader.beginObject()
                    writer.beginObject()
                    var depth = 0
                    while (reader.peek() != JsonToken.END_OBJECT || depth > 0) {
                        val token = reader.peek()
                        println(token)
                        when (token) {
                            JsonToken.BEGIN_OBJECT -> {
                                writer.beginObject()
                                depth++
                            }
                            JsonToken.END_OBJECT -> {
                                writer.endObject()
                                depth--
                            }
                            JsonToken.NAME ->
                                writer.name(reader.nextName())
                            JsonToken.BOOLEAN ->
                                writer.value(reader.nextBoolean())
                            JsonToken.STRING ->
                                writer.value(reader.nextString())
                            JsonToken.NULL ->
                                writer.nullValue()
                            JsonToken.NUMBER ->
                                writer.value(BigDecimal(reader.nextString()))
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

            sequence.map {
                println(it)
                it
            }.count() shouldBe 3
        }
    }
})
