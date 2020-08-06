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

package ca.stellardrift.permissionsex.util

import ca.stellardrift.permissionsex.context.ContextValue
import com.google.common.collect.Maps

typealias ContextSet = Set<ContextValue<*>>
typealias SubjectIdentifier = Map.Entry<String, String>

fun subjectIdentifier(type: String, name: String): SubjectIdentifier = Maps.immutableEntry(type, name)

/**
 * An iterator with a single-element lookahead
 */
interface PeekingIterator<T> : Iterator<T> {
    /**
     * See what the next element in this sequence is, or null if at the end of the sequence
     */
    fun peek(): T?
}

private class WrappingPeekingIterator<T>(val wrap: Iterator<T>) : PeekingIterator<T> {
    private var next: T? = null

    init {
        populate()
    }

    private fun populate() {
        next = null
        if (wrap.hasNext()) {
            next = wrap.next()
        }
    }

    /**
     * See what the next element in this sequence is, or null
     */
    override fun peek(): T? {
        return next
    }

    /**
     * Returns `true` if the iteration has more elements.
     */
    override fun hasNext(): Boolean {
        return next != null
    }

    /**
     * Returns the next element in the iteration.
     */
    override fun next(): T {
        val ret = next
        if (ret == null) {
            throw IndexOutOfBoundsException()
        } else {
            populate()
            return ret
        }
    }
}

/**
 * An interface that can peek ahead
 */
interface PeekingIterable<T> : Iterable<T> {
    fun peekingIterator(): PeekingIterator<T>
}

fun <T> Iterable<T>.peekingIterator(): PeekingIterator<T> {
    return iterator().peeking()
}

fun <T> Iterator<T>.peeking(): PeekingIterator<T> {
    return WrappingPeekingIterator(this)
}
