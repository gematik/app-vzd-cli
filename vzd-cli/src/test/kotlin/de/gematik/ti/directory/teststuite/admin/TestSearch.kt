package de.gematik.ti.directory.teststuite.admin

import de.gematik.ti.directory.admin.Client
import io.kotest.core.spec.style.FeatureSpec

class TestSearch : FeatureSpec({
    var client: Client? = null

    // val GIVEN_NAMES = File("ml/data/given_names.csv").readLines()
    // val FAMILYNAMES = File("ml/data/family_names.csv").readLines()

    beforeSpec {
        // client = createClient()
    }
/*
    feature("Train the models") {
        scenario("Train postalCode") {

            val samleDataset = File("ml/PLZ_2021.csv").readLines().map { line ->
                val tokens = line.split(",")
                val sample = " <START:postalCode> ${tokens[0]} <END> "
                val ort = tokens[1]
                val ortsteil = tokens[2]

                listOf(
                    sample,
                    "$sample $ort",
                    "$sample $ort $ortsteil",
                    "$sample $ort-$ortsteil",
                    "$sample, $ort",
                    "$sample, $ort-$ortsteil",
                    "$ort, $sample",
                    "$ort $sample",
                    "$ort-$ortsteil $sample",
                    "$ort-$ortsteil, $sample",
                )
            }.flatten()

            val trainedModel = NameFinderME.train("de", null,
                NameSampleDataStream(CollectionObjectStream(samleDataset)),
                TrainingParameters.defaultParams(),
                TokenNameFinderFactory());

            trainedModel.serialize(FileOutputStream("ml/model-name-postalCode.bin"))

        }
        scenario("Train localityName") {

            val samleDataset = File("ml/PLZ_2021.csv").readLines().map { line ->
                val tokens = line.split(",")
                val plz = tokens[0]
                val ort = tokens[1]
                val ortsteil = tokens[2]
                val sample1 = " <START:localityName> $ort <END> "
                val sample2 = " <START:localityName> $ort-$ortsteil <END> "

                buildList {
                    add(" $plz $sample1 ")
                    add(" $plz $sample2 ")
                    add("$sample1, $plz ")
                    add("$sample2, $plz ")


                }
            }.flatten()

            val trainedModel = NameFinderME.train("de", null,
                NameSampleDataStream(CollectionObjectStream(samleDataset)),
                TrainingParameters.defaultParams(),
                TokenNameFinderFactory());

            trainedModel.serialize(FileOutputStream("ml/model-name-localityName.bin"))

        }

    }

    feature("ML-Suche nach Einträgen") {
        scenario("Tokenize the search string") {
            runBlocking {
                val postalCodeModel = TokenNameFinderModel(FileInputStream("ml/model-name-postalCode.bin"))
                val postalCodeFinder = NameFinderME(postalCodeModel)

                val localityNameModel = TokenNameFinderModel(FileInputStream("ml/model-name-localityName.bin"))
                val localityNameFinder = NameFinderME(localityNameModel)

                val tokenModel = TokenizerModel(FileInputStream("ml/opennlp-de-ud-gsd-tokens-1.0-1.9.3.bin"))
                val tokenizer = TokenizerME(tokenModel)

                File("ml/search-queries.txt").readLines().forEach { query ->
                    val tokens = tokenizer.tokenize(query)
                    println()
                    println(tokens.toList())
                    val postalCodes = postalCodeFinder.find(tokens)
                    postalCodes.forEach {span ->
                        val postalCode = tokens.slice(span.start..span.end-1)
                        println("${span.type} $postalCode ${(span.prob*100).toInt()}%")
                    }
                    val localityNames = localityNameFinder.find(tokens)
                    localityNames.forEach {span ->
                        val localityName = tokens.slice(span.start..span.end-1)
                        println("${span.type} $localityName ${(span.prob*100).toInt()}%")
                    }
                }
            }

        }
    }
*/
})
