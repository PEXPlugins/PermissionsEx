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

import ca.stellardrift.permissionsex.commands.CommonMessages
import com.google.common.collect.Lists
import net.kyori.text.Component

/**
 * Holder for command arguments
 */
class CommandArgs(val raw: String, private var _args: MutableList<SingleArg>) {
    val args: List<SingleArg> get() = _args

    init {
        this._args = this._args.toMutableList()
    }

    /**
     * Return the position of the last next() call, or -1 if next() has never been called
     *
     * @return The current position
     */
    var position = -1

    operator fun hasNext(): Boolean {
        return position + 1 < args.size
    }

    @Throws(ArgumentParseException::class)
    fun peek(): String {
        if (!hasNext()) {
            throw createError(CommonMessages.ERROR_ARGUMENTS_NOTENOUGH.invoke())
        }
        return args[position + 1].value
    }

    @Throws(ArgumentParseException::class)
    operator fun next(): String {
        if (!hasNext()) {
            throw createError(CommonMessages.ERROR_ARGUMENTS_NOTENOUGH.invoke())
        }
        return args[++position].value
    }

    fun nextIfPresent(): String? {
        return if (hasNext()) {
            args[++position].value
        } else {
            null
        }
    }

    fun createError(message: Component): ArgumentParseException {
        // System.out.println("Creating error: " + message.translateFormatted(Locale.getDefault()));
        // Thread.dumpStack();
        return ArgumentParseException(
            message,
            raw,
            if (position < 0) 0 else args[position].startIdx
        )
    }

    val all: List<String>
        get() = Lists.transform(
            args
        ) { obj: SingleArg? -> obj!!.value }

    fun filterArgs(filter: (String) -> Boolean) {
        val currentArg =
            if (position == -1) null else _args[position]
        val newArgs: MutableList<SingleArg> = mutableListOf()
        for (arg in args) {
            if (filter(arg.value)) {
                newArgs.add(arg)
            }
        }
        position = if (currentArg == null) -1 else newArgs.indexOf(currentArg)
        _args = newArgs
    }

    fun insertArg(value: String) {
        val index = if (position < 0) 0 else args[position].endIdx
        _args.add(index,
            SingleArg(value, index, index)
        )
    }

    fun removeArgs(startIdx: Int, endIdx: Int) {
        if (position >= startIdx) {
            if (position < endIdx) {
                position = startIdx - 1
            } else {
                position -= endIdx - startIdx + 1
            }
        }
        for (i in startIdx..endIdx) {
            _args.removeAt(startIdx)
        }
    }

    data class SingleArg(val value: String, val startIdx: Int, val endIdx: Int)
}
