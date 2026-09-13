package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotEmptyCollectionField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.RequiredField
import kotlinx.serialization.SerialName

data class TestNoAnnotationsDto(val value: String)

data class TestRequiredDto(@RequiredField val field: String?)

data class TestSerialNameDto(
    @SerialName("wire_name")
    @RequiredField
    val kotlinName: String?
)

data class TestNotBlankDto(@NotBlankStringField val text: String)

data class TestNotEmptyDto(@NotEmptyCollectionField val items: List<String>)
