package io.github.mudrichenkoevgeny.backend.core.common.testing.contract

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.createTestApplicationCallWithQuery
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingParamNames
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Asserts that the backend DTO class [backendDtoClass] stays synchronized with the shared KMP DTO class [sharedDtoClass].
 *
 * @param sharedDtoClass shared KMP DTO model from shared-foundation
 * @param backendDtoClass backend DTO model used in request handlers and repositories
 * @param propertyAliases mapping for property names that intentionally differ between shared and backend models
 * @param ignoredSharedProperties properties present in shared model that are intentionally excluded from backend model
 * @param ignoredBackendProperties properties present in backend model that are intentionally excluded from shared model
 */
fun <SharedDto : Any, BackendDto : Any> assertDtoSynchronization(
    sharedDtoClass: KClass<SharedDto>,
    backendDtoClass: KClass<BackendDto>,
    propertyAliases: Map<String, String> = emptyMap(),
    ignoredSharedProperties: Set<String> = emptySet(),
    ignoredBackendProperties: Set<String> = emptySet()
) {
    val sharedProps = sharedDtoClass.memberProperties.map { it.name }.toSet()
    val backendProps = backendDtoClass.memberProperties.map { it.name }.toSet()

    val mappedSharedProps = sharedProps
        .filterNot { it in ignoredSharedProperties }
        .map { propertyAliases[it] ?: it }
        .toSet()

    val mappedBackendProps = backendProps.filterNot { it in ignoredBackendProperties }.toSet()

    check(mappedSharedProps == mappedBackendProps) {
        "Backend ${backendDtoClass.simpleName} and Shared ${sharedDtoClass.simpleName} are out of sync!\n" +
            "Expected (Shared): $mappedSharedProps\n" + "Actual (Backend): $mappedBackendProps"
    }
}

/**
 * Asserts that the query parameter parser function [parseFunction] extracts and parses all wire keys
 * defined in the [filterKeysClass] object without throwing exceptions.
 *
 * @param filterKeysClass filter keys constant object (e.g. UserFilterValues)
 * @param sampleValueProvider function providing a valid sample query value for a given wire key
 * @param parseFunction function executing the ApplicationCall query parser
 */
fun <Dto : Any> assertExhaustiveFilterKeysParsed(
    filterKeysClass: KClass<*>,
    sampleValueProvider: (String) -> String,
    parseFunction: (ApplicationCall) -> Dto
) {
    val filterKeys = filterKeysClass.memberProperties.filter { it.isConst }.mapNotNull { it.getter.call() as? String }

    check(filterKeys.isNotEmpty()) {
        "Filter keys class ${filterKeysClass.simpleName} must contain constant string wire key definitions"
    }

    val sampleParams = filterKeys.associateWith { key ->
        listOf(sampleValueProvider(key))
    }

    val parameters = Parameters.build {
        sampleParams.forEach { (key, values) ->
            appendAll(
                key,
                values
            )
        }
    }
    val call = createTestApplicationCallWithQuery(parameters)

    parseFunction(call)
}

/**
 * Asserts that every wire value in [enumEntries] for a `sort_by` enum is correctly accepted and parsed
 * by [parseFunction].
 *
 * @param enumEntries list of enum entries to test (e.g. UserSortBy.entries)
 * @param wireValueExtractor function to extract the string wire value from an enum entry
 * @param parseFunction function executing the ApplicationCall query parser for a `sort_by` wire value
 * @param assertSortByFunction assertion checking that parsed DTO contains the expected enum entry
 */
fun <SortByEnum : Enum<SortByEnum>, Dto : Any> assertAllSortByEnumEntriesParsed(
    enumEntries: List<SortByEnum>,
    wireValueExtractor: (SortByEnum) -> String,
    parseFunction: (ApplicationCall) -> Dto,
    assertSortByFunction: (Dto, SortByEnum) -> Unit
) {
    check(enumEntries.isNotEmpty()) { "SortBy enum entries list must not be empty" }

    for (entry in enumEntries) {
        val wireValue = wireValueExtractor(entry)
        val parameters = Parameters.build {
            append(
                ListingParamNames.Sort.SORT_BY,
                wireValue
            )
        }
        val call = createTestApplicationCallWithQuery(parameters)
        val result = parseFunction(call)
        assertSortByFunction(
            result,
            entry
        )
    }
}
