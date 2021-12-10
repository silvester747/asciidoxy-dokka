package org.asciidoxy.dokka

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsciiDoxyDokkaPluginTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `simple class to json`() {
        testInline(
            """
            |/src/main/kotlin/sample/Test.kt
            |package sample
            | /**
            |  * Testing is really easy.
            |  *
            |  * I am not kidding you, it is really, really easy!
            |  *
            |  * @property reason The reason this is easy.
            |  * @constructor Create an easy test.
            |  */
            |data class TestingIsEasy(val reason: String)
            """.trimIndent(), configuration
        ) {
            renderingStage = { _, context ->
                val outputFile = File(context.configuration.outputDir, "asciidoxy.json")
                assertTrue(outputFile.exists())
                assertEquals(
                    """
                        {
                            "dri": "////PointingToDeclaration/",
                            "name": "root",
                            "children": [
                                {
                                    "type": "org.asciidoxy.dokka.JsonDPackage",
                                    "dri": "sample////PointingToDeclaration/",
                                    "name": "sample",
                                    "children": [
                                        {
                                            "type": "org.asciidoxy.dokka.JsonDClasslike",
                                            "dri": "sample/TestingIsEasy///PointingToDeclaration/",
                                            "name": "TestingIsEasy",
                                            "children": [
                                                {
                                                    "type": "org.asciidoxy.dokka.JsonDProperty",
                                                    "dri": "sample/TestingIsEasy/reason/#/PointingToDeclaration/",
                                                    "name": "reason",
                                                    "visibility": "public"
                                                },
                                                {
                                                    "type": "org.asciidoxy.dokka.JsonDFunction",
                                                    "dri": "sample/TestingIsEasy/TestingIsEasy/#kotlin.String/PointingToDeclaration/",
                                                    "name": "TestingIsEasy",
                                                    "isConstructor": true,
                                                    "parameters": [
                                                        {
                                                            "type": "org.asciidoxy.dokka.JsonDParameter",
                                                            "dri": "sample/TestingIsEasy/TestingIsEasy/#kotlin.String/PointingToCallableParameters(0)/",
                                                            "name": "reason",
                                                            "parameterType": {
                                                                "type": "org.asciidoxy.dokka.JsonGenericTypeConstructor",
                                                                "dri": "kotlin/String///PointingToDeclaration/",
                                                                "projections": [
                                                                ],
                                                                "presentableName": null
                                                            }
                                                        }
                                                    ],
                                                    "visibility": "public",
                                                    "returnType": {
                                                        "type": "org.asciidoxy.dokka.JsonGenericTypeConstructor",
                                                        "dri": "sample/TestingIsEasy///PointingToDeclaration/",
                                                        "projections": [
                                                        ],
                                                        "presentableName": null
                                                    }
                                                }
                                            ],
                                            "visibility": "public",
                                            "docs": {
                                                "Description": "<MARKDOWN_FILE><P>Testing is really easy.</P><P>I am not kidding you, it is really, really easy!</P></MARKDOWN_FILE>",
                                                "Property": "<MARKDOWN_FILE><P>The reason this is easy.</P></MARKDOWN_FILE>",
                                                "Constructor": "<MARKDOWN_FILE><P>Create an easy test.</P></MARKDOWN_FILE>"
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    """.trimIndent(),
                    outputFile.readText())
            }
        }
    }
}