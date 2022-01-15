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
sealed class JsonDocumentable {
    abstract val dri: String
    abstract val name: String?
    abstract val docs: Map<String, String>
}

interface WithChildren {
    val children: List<JsonDocumentable>
}

@Serializable
data class JsonDModule(
    override val dri: String,
    override val name: String,
    override val children: List<JsonDocumentable>,
    override val docs: Map<String, String>
) : JsonDocumentable(), WithChildren

@Serializable
data class JsonDPackage(
    override val dri: String,
    override val name: String,
    override val children: List<JsonDocumentable>,
    override val docs: Map<String, String>
) : JsonDocumentable(), WithChildren

@Serializable
data class JsonDClasslike(
    override val dri: String,
    override val name: String?,
    override val children: List<JsonDocumentable>,
    val visibility: String?,
    override val docs: Map<String, String>
) : JsonDocumentable(), WithChildren

@Serializable
data class JsonDFunction(
    override val dri: String,
    override val name: String,
    val isConstructor: Boolean,
    val parameters: List<JsonDocumentable>,
    val visibility: String?,
    val returnType: JsonBound?,
    override val docs: Map<String, String>
) : JsonDocumentable()

@Serializable
data class JsonDParameter(
    override val dri: String,
    override val name: String?,
    val parameterType: JsonBound?,
    override val docs: Map<String, String>
) : JsonDocumentable()

@Serializable
data class JsonDProperty(
    override val dri: String,
    override val name: String,
    val visibility: String?,
    override val docs: Map<String, String>
) : JsonDocumentable()

@Serializable
data class JsonDEnumEntry(
    override val dri: String,
    override val name: String,
    override val children: List<JsonDocumentable>,
    override val docs: Map<String, String>
) : JsonDocumentable(), WithChildren

@Serializable
sealed class JsonProjection

@Serializable
sealed class JsonBound : JsonProjection()

@Serializable
data class JsonTypeParameter(
    val dri: String,
    val name: String,
    val presentableName: String?
) : JsonBound()

@Serializable
sealed class JsonTypeConstructor : JsonBound() {
    abstract val dri: String
    abstract val projections: List<JsonProjection>
    abstract val presentableName: String?
}

@Serializable
data class JsonGenericTypeConstructor(
    override val dri: String,
    override val projections: List<JsonProjection>,
    override val presentableName: String?
) : JsonTypeConstructor()

@Serializable
data class JsonFunctionalTypeConstructor(
    override val dri: String,
    override val projections: List<JsonProjection>,
    val isExtensionFunction: Boolean,
    val isSuspendable: Boolean,
    override val presentableName: String?
) : JsonTypeConstructor()

@Serializable
data class JsonNullable(
    val inner: JsonBound?
) : JsonBound()

@Serializable
object JsonVoid: JsonBound()

fun Documentable.toJson(): JsonDocumentable? =
    when (this) {
        is DPackage -> toJson()
        is DClass -> toJson()
        is DInterface -> toJson()
        is DObject -> toJson()
        is DAnnotation -> toJson()
        is DFunction -> toJson()
        is DParameter -> toJson()
        is DProperty -> toJson()
        is DEnum -> toJson()
        is DEnumEntry -> toJson()
        else -> run {
            // TODO: Use logger if possible
            println("Unhandled Documentable: ${this.javaClass.kotlin}")
            null
        }
    }

fun DModule.toJson() = JsonDModule(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DPackage.toJson() = JsonDPackage(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DClass.toJson() = JsonDClasslike(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    selectVisibility(visibility),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DInterface.toJson() = JsonDClasslike(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    selectVisibility(visibility),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DObject.toJson() = JsonDClasslike(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    selectVisibility(visibility),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DAnnotation.toJson() = JsonDClasslike(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    selectVisibility(visibility),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DEnum.toJson() = JsonDClasslike(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    selectVisibility(visibility),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DFunction.toJson() = JsonDFunction(
    dri.toString(),
    name,
    isConstructor,
    children.mapNotNull { it.toJson() },
    selectVisibility(visibility),
    type.toJson(),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DParameter.toJson() = JsonDParameter(
    dri.toString(),
    name,
    type.toJson(),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DProperty.toJson() = JsonDProperty(
    dri.toString(),
    name,
    selectVisibility(visibility),
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun DEnumEntry.toJson() = JsonDEnumEntry(
    dri.toString(),
    name,
    children.mapNotNull { it.toJson() },
    documentation.forDefaultPlatform()?.collectDocumentation() ?: emptyMap()
)

fun Bound.toJson(): JsonBound? = when (this) {
    is TypeParameter -> toJson()
    is GenericTypeConstructor -> toJson()
    is FunctionalTypeConstructor -> toJson()
    is Nullable -> toJson()
    is Void -> toJson()
    else -> null
}

fun Projection.toJson(): JsonProjection? = when (this) {
    is Bound -> toJson()
    else -> null
}

fun TypeParameter.toJson() = JsonTypeParameter(
    dri.toString(),
    name,
    presentableName
)

fun GenericTypeConstructor.toJson() = JsonGenericTypeConstructor(
    dri.toString(),
    projections.mapNotNull { it.toJson() },
    presentableName
)

fun FunctionalTypeConstructor.toJson() = JsonFunctionalTypeConstructor(
    dri.toString(),
    projections.mapNotNull { it.toJson() },
    isExtensionFunction,
    isSuspendable,
    presentableName
)

fun Nullable.toJson() = JsonNullable(
    inner.toJson()
)

fun Void.toJson() = JsonVoid

fun <T> SourceSetDependent<T>.forDefaultPlatform(): T? =
    this.filterKeys {it.analysisPlatform == Platform.DEFAULT }.values.firstOrNull()

fun selectVisibility(visibility: SourceSetDependent<Visibility>) =
    visibility.forDefaultPlatform()?.name

fun DocumentationNode.collectDocumentation() =
    children.associate { Pair(it.docName(), it.root.render()) }

fun TagWrapper.docName(): String {
    if (this is NamedTagWrapper) {
        return "${javaClass.simpleName}: $name"
    }
    return javaClass.simpleName
}

fun DocTag.render(): String {
    if (this is Text) {
        return body
    }

    val tagName = when (this) {
        is CustomDocTag -> name
        else -> javaClass.simpleName
    }
    val driAttribute = when (this) {
        is DocumentationLink -> " dri=\"${dri}\""
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