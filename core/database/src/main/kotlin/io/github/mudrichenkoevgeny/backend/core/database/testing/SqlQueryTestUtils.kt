package io.github.mudrichenkoevgeny.backend.core.database.testing

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Custom [SqlLogger] that captures all generated SQL statements and expanded arguments
 * during transaction execution.
 */
class CapturingSqlLogger : SqlLogger {
    val statements = mutableListOf<String>()

    override fun log(context: StatementContext, transaction: Transaction) {
        statements.add(context.expandArgs(transaction))
    }

    fun clear() {
        statements.clear()
    }
}

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
suspend fun <QueryParams : Any> assertAllQueryParamsApplied(
    queryParamsClass: KClass<QueryParams>,
    defaultInstance: QueryParams,
    ignoredProperties: Set<String> = emptySet(),
    executeRepoCall: suspend (QueryParams) -> Unit
) {
    val logger = CapturingSqlLogger()
    TransactionManager.current().addLogger(logger)

    // Step 1: Capture baseline SQL statement with default QueryParams
    logger.clear()
    executeRepoCall(defaultInstance)
    val baselineSql = logger.statements.joinToString("\n")

    // Step 2: Iterate through all member properties and test each property mutation
    val properties = queryParamsClass.memberProperties
        .filter { it.name !in ignoredProperties }

    check(properties.isNotEmpty()) {
        "QueryParams class ${queryParamsClass.simpleName} must have properties to test"
    }

    for (prop in properties) {
        val sampleValue = createSampleValueForType(prop.returnType, prop.name)
            ?: continue

        val mutatedInstance = defaultInstance.copyValue(prop, sampleValue)

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
@Suppress("UNCHECKED_CAST")
fun <T : Any> T.copyValue(property: KProperty1<T, *>, newValue: Any?): T {
    val kClass = this::class as KClass<T>
    val primaryConstructor = kClass.primaryConstructor
        ?: throw IllegalArgumentException("Class ${kClass.simpleName} must have a primary constructor")

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
fun createSampleValueForType(type: KType, propertyName: String): Any? {
    val classifier = type.classifier as? KClass<*> ?: return null

    return when {
        classifier == String::class -> "sample_${propertyName}_val"
        classifier == Int::class -> 9999
        classifier == Long::class -> 8888L
        classifier == Boolean::class -> true
        classifier.java.isEnum -> classifier.java.enumConstants.let { constants ->
            if (constants.size > 1) constants[1] else constants.firstOrNull()
        }
        classifier == ListingQueryParams::class -> {
            val sortByType = type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
            val sortBySample = if (sortByType != null && sortByType.java.isEnum) {
                val constants = sortByType.java.enumConstants
                if (constants.size > 1) constants[1] else constants.firstOrNull()
            } else {
                null
            }

            if (sortBySample != null) {
                ListingQueryParams(
                    pageParams = PageParams(page = 2, size = 15),
                    sortBy = sortBySample,
                    sortOrder = SortOrder.DESC
                )
            } else {
                null
            }
        }
        classifier == List::class || classifier == Set::class -> {
            val elementType = type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
            val sampleElement = if (elementType != null && elementType.java.isEnum) {
                elementType.java.enumConstants.firstOrNull()
            } else if (elementType != null) {
                try {
                    val constructor = elementType.primaryConstructor
                    if (constructor != null && constructor.parameters.size == 1
                        && constructor.parameters.first().type.classifier == String::class
                    ) {
                        constructor.call("sample_perm")
                    } else {
                        "sample_item"
                    }
                } catch (e: Exception) {
                    "sample_item"
                }
            } else {
                "sample_item"
            }

            if (sampleElement != null) {
                if (classifier == Set::class) setOf(sampleElement) else listOf(sampleElement)
            } else {
                null
            }
        }
        else -> null
    }
}
