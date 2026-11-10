package io.github.mudrichenkoevgeny.backend.core.database.testing

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Asserts that every property in [queryParamsClass] (excluding [ignoredProperties]) alters
 * the generated SQL query (WHERE, ORDER BY, or JOIN clauses) when passed to [executeRepoCall].
 *
 * Runs inside an Exposed transaction, intercepts SQL queries via [CapturingSqlLogger], and compares
 * the mutated property SQL against the baseline SQL.
 *
 * @param queryParamsClass KClass of the QueryParams data class (e.g. `UserListQueryParams::class`)
 * @param defaultInstance instance of [QueryParams] with default/empty filter values
 * @param ignoredProperties property names that do not directly alter SQL queries (e.g. `"parser"`)
 * @param executeRepoCall suspend lambda executing the repository query method with given [QueryParams]
 */
public suspend fun <QueryParams : Any> assertAllQueryParamsApplied(
    queryParamsClass: KClass<QueryParams>,
    defaultInstance: QueryParams,
    ignoredProperties: Set<String> = emptySet(),
    executeRepoCall: suspend (QueryParams) -> Unit
) {
    val logger = CapturingSqlLogger()
    TransactionManager.current().addLogger(logger)

    logger.clear()
    executeRepoCall(defaultInstance)
    val baselineSql = logger.statements.joinToString("\n")

    val properties = queryParamsClass.memberProperties.filter { it.name !in ignoredProperties }

    check(properties.isNotEmpty()) {
        "QueryParams class ${queryParamsClass.simpleName} must have properties to test"
    }

    for (prop in properties) {
        val sampleValue = createSampleValueForType(
            prop.returnType,
            prop.name
        ) ?: continue

        val mutatedInstance = defaultInstance.copyValue(
            prop,
            sampleValue
        )

        logger.clear()
        executeRepoCall(mutatedInstance)
        val mutatedSql = logger.statements.joinToString("\n")

        check(baselineSql != mutatedSql) {
            "Property '${prop.name}' in ${queryParamsClass.simpleName} did NOT alter the generated SQL query!\n" +
                "Did you forget to apply this parameter in the repository query builder?\n" +
                "Baseline SQL: $baselineSql\n" +
                "Mutated SQL: $mutatedSql"
        }
    }
}

/**
 * Creates a new instance of a data class [T] with [property] set to [newValue].
 */
public fun <T : Any> T.copyValue(
    property: KProperty1<T, *>,
    newValue: Any?
): T {
    @Suppress("UNCHECKED_CAST") val kClass = this::class as KClass<T>
    val primaryConstructor = kClass.primaryConstructor ?: throw IllegalArgumentException(
        "Class ${kClass.simpleName} must have a primary constructor"
    )

    val args = primaryConstructor.parameters.associateWith { parameter ->
        if (parameter.name == property.name) {
            newValue
        } else {
            kClass.memberProperties.first { it.name == parameter.name }.get(this)
        }
    }

    return primaryConstructor.callBy(args)
}

/**
 * Generates a non-default sample value for a given [KType] and [propertyName].
 */
public fun createSampleValueForType(
    type: KType,
    propertyName: String
): Any? {
    val classifier = type.classifier as? KClass<*> ?: return null

    return when {
        classifier == String::class -> createStringSample(propertyName)
        classifier == Int::class -> 9999
        classifier == Long::class -> 8888L
        classifier == Boolean::class -> true
        classifier == Instant::class -> Clock.System.now()
        classifier.java.isEnum -> createEnumSample(classifier)
        classifier == ListingQueryParams::class -> createListingQueryParamsSample(type)
        classifier == List::class || classifier == Set::class -> createCollectionSample(classifier, type, propertyName)
        else -> createSampleInstanceForClass(classifier)
    }
}

private fun createStringSample(propertyName: String): String = if (propertyName.endsWith(
        "Id",
        ignoreCase = true
    ) ||
    propertyName.endsWith(
        "Ids",
        ignoreCase = true
    )
) {
    "00000000-0000-0000-0000-000000000001"
} else {
    "sample_${propertyName}_val"
}

private fun createEnumSample(classifier: KClass<*>): Any? {
    val constants = classifier.java.enumConstants
    return if (constants.size > 1) constants[1] else constants.firstOrNull()
}

private fun createListingQueryParamsSample(type: KType): Any? {
    val sortByType = type.arguments
        .firstOrNull()
        ?.type
        ?.classifier as? KClass<*>
    val sortBySample = if (sortByType != null && sortByType.java.isEnum) {
        val constants = sortByType.java.enumConstants
        if (constants.size > 1) constants[1] else constants.firstOrNull()
    } else {
        null
    }

    return if (sortBySample != null) {
        ListingQueryParams(
            pageParams = PageParams(
                page = 2,
                size = 15
            ),
            sortBy = sortBySample,
            sortOrder = SortOrder.DESC
        )
    } else {
        null
    }
}

private fun createCollectionSample(
    classifier: KClass<*>,
    type: KType,
    propertyName: String
): Any {
    val elementType = type.arguments
        .firstOrNull()
        ?.type
        ?.classifier as? KClass<*>
    val sampleElement = if (elementType != null) {
        if (elementType == String::class &&
            (
                propertyName.endsWith(
                    "Id",
                    ignoreCase = true
                ) ||
                    propertyName.endsWith(
                        "Ids",
                        ignoreCase = true
                    )
                )
        ) {
            "00000000-0000-0000-0000-000000000001"
        } else {
            createSampleInstanceForClass(elementType)
        }
    } else {
        "sample_item"
    }

    return if (classifier == Set::class) setOf(sampleElement) else listOf(sampleElement)
}

/**
 * Creates a non-null sample instance of [kClass] for reflection-based query testing.
 */
public fun createSampleInstanceForClass(kClass: KClass<*>): Any {
    createPrimitiveSample(kClass)?.let { return it }

    createInterfaceProxy(kClass)?.let { return it }

    createCompanionInstance(kClass)?.let { return it }

    createConstructorInstance(kClass)?.let { return it }

    if (kClass.simpleName == "Uuid") {
        return Uuid.random()
    }

    return "sample_item"
}

private fun createPrimitiveSample(kClass: KClass<*>): Any? = when {
    kClass == String::class -> "sample_item"
    kClass == Int::class -> 9999
    kClass == Long::class -> 8888L
    kClass == Boolean::class -> true
    kClass == Instant::class -> Clock.System.now()
    kClass.java.isEnum -> kClass.java.enumConstants.firstOrNull()
    else -> null
}

private fun createInterfaceProxy(kClass: KClass<*>): Any? {
    if (!kClass.java.isInterface) return null
    return try {
        Proxy.newProxyInstance(
            kClass.java.classLoader,
            arrayOf(kClass.java)
        ) { _, method, _ ->
            when (method.name) {
                "getValue" -> "sample_item"
                "toString" -> "sample_item"
                "hashCode" -> 123
                "equals" -> false
                else -> "sample_item"
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun createCompanionInstance(kClass: KClass<*>): Any? {
    return try {
        val companionObj = kClass.companionObjectInstance ?: return null
        val generateFunc = companionObj::class.memberFunctions.firstOrNull {
            it.name == "generate" && it.parameters.size == 1
        }
        generateFunc?.call(companionObj)
    } catch (_: Exception) {
        null
    }
}

private fun createConstructorInstance(kClass: KClass<*>): Any? {
    return try {
        val constructor = kClass.primaryConstructor ?: return null
        if (constructor.parameters.size == 1) {
            val paramType = constructor.parameters
                .first()
                .type.classifier as? KClass<*>
            if (paramType != null) {
                val paramSample = createSampleInstanceForClass(paramType)
                return constructor.call(paramSample)
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}
