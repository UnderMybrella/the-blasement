package dev.brella.blasement

import dev.brella.kornea.blaseball.base.common.BlaseballUUID
import dev.brella.kornea.blaseball.base.common.UUID
import dev.brella.kornea.blaseball.base.common.jvm
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.bind as bindNullable

/**
 * Bind a non-`null` value to a parameter identified by its
 * `index`. `value` can be either a scalar value or [Parameter].
 * @param index zero based index to bind the parameter to
 * @param value either a scalar value or [Parameter]
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAs(index: String, value: UUID): DatabaseClient.GenericExecuteSpec =
    bind(index, value.jvm)

/**
 * Bind a non-`null` value to a parameter identified by its `name`.
 * @param name the name of the parameter
 * @param value the value to bind
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAs(index: Int, value: UUID): DatabaseClient.GenericExecuteSpec =
    bind(index, value.jvm)

/**
 * Bind a non-`null` value to a parameter identified by its
 * `index`. `value` can be either a scalar value or [Parameter].
 * @param index zero based index to bind the parameter to
 * @param value either a scalar value or [Parameter]
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAs(index: String, value: BlaseballUUID): DatabaseClient.GenericExecuteSpec =
    bind(index, value.uuid.jvm)

/**
 * Bind a non-`null` value to a parameter identified by its `name`.
 * @param name the name of the parameter
 * @param value the value to bind
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAs(index: Int, value: BlaseballUUID): DatabaseClient.GenericExecuteSpec =
    bind(index, value.uuid.jvm)



/**
 * Bind a non-`null` value to a parameter identified by its
 * `index`. `value` can be either a scalar value or [Parameter].
 * @param index zero based index to bind the parameter to
 * @param value either a scalar value or [Parameter]
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAsNullable(index: String, value: UUID?): DatabaseClient.GenericExecuteSpec =
    bindNullable(index, value?.jvm)

/**
 * Bind a non-`null` value to a parameter identified by its `name`.
 * @param name the name of the parameter
 * @param value the value to bind
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAsNullable(index: Int, value: UUID?): DatabaseClient.GenericExecuteSpec =
    bindNullable(index, value?.jvm)

/**
 * Bind a non-`null` value to a parameter identified by its
 * `index`. `value` can be either a scalar value or [Parameter].
 * @param index zero based index to bind the parameter to
 * @param value either a scalar value or [Parameter]
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAsNullable(index: String, value: BlaseballUUID?): DatabaseClient.GenericExecuteSpec =
    bindNullable(index, value?.uuid?.jvm)

/**
 * Bind a non-`null` value to a parameter identified by its `name`.
 * @param name the name of the parameter
 * @param value the value to bind
 */
inline fun DatabaseClient.GenericExecuteSpec.bindAsNullable(index: Int, value: BlaseballUUID?): DatabaseClient.GenericExecuteSpec =
    bindNullable(index, value?.uuid?.jvm)