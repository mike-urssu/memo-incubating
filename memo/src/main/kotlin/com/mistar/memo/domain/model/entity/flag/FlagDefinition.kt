package com.mistar.memo.domain.model.entity.flag

interface FlagDefinition {
    val value: Int
}

infix fun FlagDefinition.and(flag: FlagDefinition): FlagDefinition {
    return FlagDefinitionImpl(flag.value or value)
}