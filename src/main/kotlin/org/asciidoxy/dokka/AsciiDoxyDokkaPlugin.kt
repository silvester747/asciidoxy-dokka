package org.asciidoxy.dokka

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import java.io.File

class AsciiDoxyDokkaPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val exporter by extending {
        CoreExtensions.documentableTransformer providing ::ModelExporter
    }
}

class ModelExporter(val context: DokkaContext) : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule {
        // TODO: Make configurable
        val jsonFile = File(context.configuration.outputDir, "asciidoxy.json")
        context.logger.info("Exporting model to $jsonFile")
        val jsonFormat = Json { prettyPrint=true }
        jsonFormat.encodeToStream(original.toJson(), jsonFile.outputStream())
        return original
    }
}

@Serializable
sealed class JsonBase {
    abstract val dri: String
    abstract val name: String?
}

@Serializable
data class JsonModule(
    override val dri: String,
    override val name: String,
    val children: List<JsonBase>
) : JsonBase()

@Serializable
data class JsonPackage(
    override val dri: String,
    override val name: String,
    val children: List<JsonBase>
) : JsonBase()

@Serializable
data class JsonClasslike(
    override val dri: String,
    override val name: String?,
    val children: List<JsonBase>,
    val visibility: String?,
    val docs: Map<String, String>
) : JsonBase()

@Serializable
data class JsonFunction(
    override val dri: String,
    override val name: String,
    val isConstructor: Boolean,
    val parameters: List<JsonBase>,
    val visibility: String?
) : JsonBase()

@Serializable
data class JsonParameter(
    override val dri: String,
    override val name: String?
) : JsonBase()

@Serializable
data class JsonProperty(
    override val dri: String,
    override val name: String,
    val visibility: String?
) : JsonBase()

@Serializable
data class JsonEnumEntry(
    override val dri: String,
    override val name: String,
    val children: List<JsonBase>
) : JsonBase()

fun Documentable.toJson(): JsonBase? =
    when (this) {
        is DPackage -> this.toJson()
        is DClass -> this.toJson()
        is DInterface -> this.toJson()
        is DObject -> this.toJson()
        is DAnnotation -> this.toJson()
        is DFunction -> this.toJson()
        is DParameter -> this.toJson()
        is DProperty -> this.toJson()
        is DEnum -> this.toJson()
        is DEnumEntry -> this.toJson()
        else -> this.run {
            // TODO: Use logger if possible
            println("Unhandled Documentable: ${this.javaClass.kotlin}")
            null
        }
    }

fun DModule.toJson() = JsonModule(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() }
)

fun DPackage.toJson() = JsonPackage(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() }
)

fun DClass.toJson() = JsonClasslike(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() },
    selectVisibility(this.visibility),
    this.documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DInterface.toJson() = JsonClasslike(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() },
    selectVisibility(this.visibility),
    this.documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DObject.toJson() = JsonClasslike(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() },
    selectVisibility(this.visibility),
    this.documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DAnnotation.toJson() = JsonClasslike(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() },
    selectVisibility(this.visibility),
    this.documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DEnum.toJson() = JsonClasslike(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() },
    selectVisibility(this.visibility),
    this.documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DFunction.toJson() = JsonFunction(
    this.dri.toString(),
    this.name,
    this.isConstructor,
    this.children.mapNotNull { it.toJson() },
    selectVisibility(this.visibility)
)

fun DParameter.toJson() = JsonParameter(this.dri.toString(), this.name)

fun DProperty.toJson() = JsonProperty(
    this.dri.toString(),
    this.name,
    selectVisibility(this.visibility)
)

fun DEnumEntry.toJson() = JsonEnumEntry(
    this.dri.toString(),
    this.name,
    this.children.mapNotNull { it.toJson() }
)

fun <T> SourceSetDependent<T>.forDefaultPlatform(): T? =
    this.filterKeys {it.analysisPlatform == Platform.DEFAULT }.values.firstOrNull()

fun selectVisibility(visibility: SourceSetDependent<Visibility>) =
    visibility.forDefaultPlatform()?.name

fun DocumentationNode.collectDocumentation() =
    children.associate { Pair(it.javaClass.simpleName, it.root.render()) }

fun DocTag.render(): String {
    if (this is Text) {
        return body
    }

    val tagName = when (this) {
        is CustomDocTag -> name
        else -> javaClass.simpleName
    }
    val driAttribute = when (this) {
        is DocumentationLink -> " dri=\"${dri.toString()}\""
        else -> ""
    }
    val attributes = params.map { "${it.key}=\"${it.value}\""}.joinToString(separator=" ", prefix=" ").trim()

    return if (children.isEmpty()) {
        "<${tagName}${attributes}$driAttribute />"
    } else {
        val childContent = children.map { it.render() }.reduce(String::plus)
        "<${tagName}${attributes}$driAttribute>$childContent</${tagName}>"
    }
}