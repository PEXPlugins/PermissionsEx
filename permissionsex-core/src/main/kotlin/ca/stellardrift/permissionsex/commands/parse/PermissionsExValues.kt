/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.permissionsex.commands.parse

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.Messages
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.FlagCommandElementBuilder
import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.commands.ArgumentKeys.CONTEXT_ERROR_FORMAT
import ca.stellardrift.permissionsex.commands.ArgumentKeys.CONTEXT_ERROR_TYPE
import ca.stellardrift.permissionsex.commands.ArgumentKeys.CONTEXT_ERROR_VALUE
import ca.stellardrift.permissionsex.commands.ArgumentKeys.SUBJECTTYPE_ERROR_NOTATYPE
import ca.stellardrift.permissionsex.commands.ArgumentKeys.SUBJECT_ERROR_NAMEINVALID
import ca.stellardrift.permissionsex.util.subjectIdentifier
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.Component
import java.util.concurrent.CompletableFuture


fun contextTransientFlags(pex: PermissionsEx<*>): FlagCommandElementBuilder {
    return StructuralArguments.flags()
        .flag("-transient")
        .valueFlag(context(pex).key(Messages.COMMON_ARGS_CONTEXT()), "-context", "-contexts", "c")
}

fun subjectType(pex: PermissionsEx<*>): Value<String> = SubjectTypeValue(pex)

class SubjectTypeValue(private val pex: PermissionsEx<*>) : Value<String>(+"") {
    @Throws(ArgumentParseException::class)
    override fun parse(args: CommandArgs): String {
        val next = args.next()
        val subjectTypes = pex.registeredSubjectTypes
        if (!subjectTypes.contains(next)) {
            throw args.createError(SUBJECTTYPE_ERROR_NOTATYPE.invoke(next))
        }
        return next
    }

    override fun tabComplete(
        src: Commander,
        args: CommandArgs
    ): Sequence<String> {
        val seq = pex.registeredSubjectTypes.asSequence()
        val nextOpt = args.nextIfPresent()?: return seq
        return seq.filter { it.startsWith(nextOpt, ignoreCase = true)}
    }

}

/**
 * Expect the provided argument to specify a subject. Subject is of one of the forms:
 *
 *  * &lt;type&gt;:&lt;identifier&gt;
 *  * &lt;type&gt; &lt;identifier&gt;
 *
 * @param pex The PermissionsEx instance to fetch known subjects from
 * @return the element to match the input
 */
fun subject(
    pex: PermissionsEx<*>,
    defaultType: String? = null
): Value<SubjectIdentifier> {
    return SubjectElement(pex, defaultType)
}

private class SubjectElement(
    private val pex: PermissionsEx<*>,
    private val defaultType: String?
) : Value<SubjectIdentifier>(+"a subject, identified by its type followed ") {
    @Throws(ArgumentParseException::class)
    override fun parse(args: CommandArgs): SubjectIdentifier {
        var type = args.next()
        var identifier: String?
        if (type.contains(":")) {
            val typeSplit = type.split(":", limit =2)
            type = typeSplit[0]
            identifier = typeSplit[1]
        } else if (!args.hasNext() && defaultType != null) {
            identifier = type
            type = defaultType
        } else {
            identifier = args.next()
        }
        val subjType = pex.getSubjects(type)
        if (!subjType.isRegistered(identifier).join()) { // TODO: Async command elements
            val newIdentifier = subjType.typeInfo.getAliasForName(identifier)
            if (newIdentifier.isPresent) {
                identifier = newIdentifier.get()
            }
        }
        if (!subjType.typeInfo.isNameValid(identifier)) {
            throw args.createError(SUBJECT_ERROR_NAMEINVALID.invoke(identifier, type))
        }
        return subjectIdentifier(type, identifier)
    }

    override fun tabComplete(
        src: Commander,
        args: CommandArgs
    ): Sequence<String> {
        var type = args.nextIfPresent() ?: return pex.registeredSubjectTypes.asSequence()
        var identifierSegment = args.nextIfPresent()
        if (identifierSegment == null) { // TODO: Correct tab completion logic
            return if (type.contains(":")) {
                val argSplit = type.split(":", limit = 2).toTypedArray()
                type = argSplit[0]
                identifierSegment = argSplit[1]
                val typeObj = pex.getSubjects(type)
                val allIdents = typeObj.allIdentifiers.asSequence()
                (allIdents +
                        allIdents.map {typeObj.typeInfo.getAliasForName(it).orElse(null) }.filterNotNull())
                    .filter { it.startsWith(identifierSegment, ignoreCase = true)}
                    .map { "${typeObj.typeInfo.typeName}:$it"}
            } else {
                pex.registeredSubjectTypes.asSequence()
                    .filter { it.startsWith(type, ignoreCase = true) }
            }
        }
        val typeObj = pex.getSubjects(type)
        val allIdents = typeObj.allIdentifiers.asSequence()
        return (allIdents +
                allIdents.map {typeObj.typeInfo.getAliasForName(it).orElse(null) }.filterNotNull())
            .filter { it.startsWith(identifierSegment, ignoreCase = true)}
    }

}

fun context(pex: PermissionsEx<*>): Value<ContextValue<*>> = ContextCommandValue(pex)

private class ContextCommandValue(private val pex: PermissionsEx<*>) : Value<ContextValue<*>>(+"the value of a context") {
    override fun parse(args: CommandArgs): ContextValue<*> {
        val context = args.next() // TODO: Allow multi-word contexts (<key> <value>)
        val contextSplit = context.split("=", limit = 2).toTypedArray()
        if (contextSplit.size != 2) {
            throw args.createError(CONTEXT_ERROR_FORMAT.invoke())
        }
        val def =
            pex.getContextDefinition(contextSplit[0])
                ?: throw args.createError(CONTEXT_ERROR_TYPE.invoke(contextSplit[0]))
        return toCtxValue(def, contextSplit[1]) ?: throw args.createError(CONTEXT_ERROR_VALUE.invoke(context))
    }

    private fun <T> toCtxValue(
        def: ContextDefinition<T>,
        input: String
    ): ContextValue<T>? {
        val value = def.deserialize(input)
        return if (value == null) {
            null
        } else {
            def.createValue(value)
        }
    }

    override fun tabComplete(src: Commander, args: CommandArgs): Sequence<String> {
        return super.tabComplete(src, args) // TODO: Tab complete context types
    }

    override fun usage(key: Component) = +"<context-type>=<value>"

}

fun rankLadder(pex: PermissionsEx<*>): Value<CompletableFuture<RankLadder>> {
    return RankLadderValue(pex)
}

private class RankLadderValue(private val pex: PermissionsEx<*>): Value<CompletableFuture<RankLadder>>(+"the name of a rank ladder") {
    override fun parse(args: CommandArgs): CompletableFuture<RankLadder> {
        return pex.ladders[args.next(), null]
    }

    override fun tabComplete(src: Commander, args: CommandArgs): Sequence<String> {
        val arg = args.nextIfPresent() ?: return pex.ladders.all.asSequence()
        return pex.ladders.all.asSequence()
            .filter {it.startsWith(arg, ignoreCase = true) }
    }
}

fun permission(pex: PermissionsEx<*>) =
    suggestibleString(
        { pex.recordingNotifier.knownPermissions.asSequence() },
        +"any permission, suggesting from those that have been checked before"
    )

fun option(pex: PermissionsEx<*>) = suggestibleString(
    { pex.recordingNotifier.knownOptions.asSequence() },
    +"any option, suggesting from those that have already been checked"
)

fun permissionValue(): Value<Int> {
    return int() or (boolean()
        .map { if (it) 1 else -1 } or choices(
        mapOf("none" to 0, "null" to 0, "unset" to 0), +"no value"
    ))
}
