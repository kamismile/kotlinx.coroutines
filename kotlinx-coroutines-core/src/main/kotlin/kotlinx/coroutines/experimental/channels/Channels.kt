/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental.channels

import kotlinx.coroutines.experimental.Unconfined
import kotlin.coroutines.experimental.CoroutineContext

internal const val DEFAULT_CLOSE_MESSAGE = "Channel was closed"

// -------- Conversions to ReceiveChannel  --------

/**
 * Returns a channel to read all element of the [Iterable].
 */
public fun <E> Iterable<E>.asReceiveChannel(context: CoroutineContext = Unconfined): ReceiveChannel<E> =
    produce(context) {
        for (element in this@asReceiveChannel)
            send(element)
    }

/**
 * Returns a channel to read all element of the [Sequence].
 */
public fun <E> Sequence<E>.asReceiveChannel(context: CoroutineContext = Unconfined): ReceiveChannel<E> =
    produce(context) {
        for (element in this@asReceiveChannel)
            send(element)
    }

// -------- Operations on BroadcastChannel --------

/**
 * Opens subscription to this [BroadcastChannel] and makes sure that the given [block] consumes all elements
 * from it by always invoking [close][SubscriptionReceiveChannel.close] after the execution of the block.
 */
public inline fun <E, R> BroadcastChannel<E>.consume(block: SubscriptionReceiveChannel<E>.() -> R): R =
    openSubscription().use { channel ->
        channel.block()
    }

/**
 * Subscribes to this [BroadcastChannel] and performs the specified action for each received element.
 */
public inline suspend fun <E> BroadcastChannel<E>.consumeEach(action: (E) -> Unit) =
    consume {
        for (element in this) action(element)
    }

// -------- Operations on ReceiveChannel --------

/**
 * Makes sure that the given [block] consumes all elements from the given channel
 * by always invoking [consumeAll][ReceiveChannel.consumeAll] after the execution of the block.
 */
public inline fun <E, R> ReceiveChannel<E>.consume(block: ReceiveChannel<E>.() -> R): R =
    try {
        block()
    } finally {
        consumeAll()
    }

/**
 * Performs the given [action] for each received element.
 *
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <E> ReceiveChannel<E>.consumeEach(action: (E) -> Unit) =
    consume {
        for (element in this) action(element)
    }

/**
 * @suppress: **Deprecated**: binary compatibility with old code
 */
@Deprecated("binary compatibility with old code", level = DeprecationLevel.HIDDEN)
public suspend fun <E> ReceiveChannel<E>.consumeEach(action: suspend (E) -> Unit) =
    consumeEach { action(it) }

/**
 * @suppress: **Deprecated**: binary compatibility with old code
 */
@Deprecated("binary compatibility with old code", level = DeprecationLevel.HIDDEN)
public suspend fun <E> BroadcastChannel<E>.consumeEach(action: suspend (E) -> Unit) =
    consumeEach { action(it) }

/**
 * Performs the given [action] for each received element.
 *
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <E> ReceiveChannel<E>.consumeEachIndexed(action: (IndexedValue<E>) -> Unit) {
    var index = 0
    consumeEach {
        action(IndexedValue(index++, it))
    }
}

/**
 * Consumes all elements from this channel and adds them to the given destination collection.
 *
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T, E : T> ReceiveChannel<E>.consumeTo(destination: MutableCollection<T>) {
    consumeEach {
        destination += it
    }
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.elementAt(index: Int): T =
    elementAtOrElse(index) { throw IndexOutOfBoundsException("ReceiveChannel doesn't contain element at index $index.") }

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.elementAtOrElse(index: Int, defaultValue: (Int) -> T): T =
    consume {
        if (index < 0)
            return defaultValue(index)
        var count = 0
        for (element in this) {
            if (index == count++)
                return element
        }
        return defaultValue(index)
    }

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.elementAtOrNull(index: Int): T? =
    consume {
        if (index < 0)
            return null
        var count = 0
        for (element in this) {
            if (index == count++)
                return element
        }
        return null
    }

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.find(predicate: (T) -> Boolean): T? =
    firstOrNull(predicate)

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.findLast(predicate: (T) -> Boolean): T? =
    lastOrNull(predicate)

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the channel is empty.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.first(): T =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext())
            throw NoSuchElementException("ReceiveChannel is empty.")
        return iterator.next()
    }

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.first(predicate: (T) -> Boolean): T {
    consumeEach {
        if (predicate(it)) return it
    }
    throw NoSuchElementException("ReceiveChannel contains no element matching the predicate.")
}

/**
 * Returns the first element, or `null` if the channel is empty.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.firstOrNull(): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext())
            return null
        return iterator.next()
    }

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.firstOrNull(predicate: (T) -> Boolean): T? {
    consumeEach {
        if (predicate(it)) return it
    }
    return null
}

/**
 * Returns first index of [element], or -1 if the channel does not contain element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.indexOf(element: T): Int {
    var index = 0
    consumeEach {
        if (element == it)
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the channel does not contain such element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    var index = 0
    consumeEach {
        if (predicate(it))
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the channel does not contain such element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.indexOfLast(predicate: (T) -> Boolean): Int {
    var lastIndex = -1
    var index = 0
    consumeEach {
        if (predicate(it))
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the channel is empty.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.last(): T =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext())
            throw NoSuchElementException("ReceiveChannel is empty.")
        var last = iterator.next()
        while (iterator.hasNext())
            last = iterator.next()
        return last
    }

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.last(predicate: (T) -> Boolean): T {
    var last: T? = null
    var found = false
    consumeEach {
        if (predicate(it)) {
            last = it
            found = true
        }
    }
    if (!found) throw NoSuchElementException("ReceiveChannel contains no element matching the predicate.")
    @Suppress("UNCHECKED_CAST")
    return last as T
}

/**
 * Returns last index of [element], or -1 if the channel does not contain element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.lastIndexOf(element: T): Int {
    var lastIndex = -1
    var index = 0
    consumeEach {
        if (element == it)
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns the last element, or `null` if the channel is empty.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.lastOrNull(): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext())
            return null
        var last = iterator.next()
        while (iterator.hasNext())
            last = iterator.next()
        return last
    }

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.lastOrNull(predicate: (T) -> Boolean): T? {
    var last: T? = null
    consumeEach {
        if (predicate(it)) {
            last = it
        }
    }
    return last
}

/**
 * Returns the single element, or throws an exception if the channel is empty or has more than one element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.single(): T =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext())
            throw NoSuchElementException("ReceiveChannel is empty.")
        val single = iterator.next()
        if (iterator.hasNext())
            throw IllegalArgumentException("ReceiveChannel has more than one element.")
        return single
    }

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.single(predicate: (T) -> Boolean): T {
    var single: T? = null
    var found = false
    consumeEach {
        if (predicate(it)) {
            if (found) throw IllegalArgumentException("ReceiveChannel contains more than one matching element.")
            single = it
            found = true
        }
    }
    if (!found) throw NoSuchElementException("ReceiveChannel contains no element matching the predicate.")
    @Suppress("UNCHECKED_CAST")
    return single as T
}

/**
 * Returns single element, or `null` if the channel is empty or has more than one element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.singleOrNull(): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext())
            return null
        val single = iterator.next()
        if (iterator.hasNext())
            return null
        return single
    }

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    var single: T? = null
    var found = false
    consumeEach {
        if (predicate(it)) {
            if (found) return null
            single = it
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns a channel containing all elements except first [n] elements.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.drop(n: Int, context: CoroutineContext = Unconfined): ReceiveChannel<T> =
    produce(context) {
        consume {
            require(n >= 0) { "Requested element count $n is less than zero." }
            var remaining: Int = n
            if (remaining > 0)
                for (element in this@drop) {
                    remaining--
                    if (remaining == 0)
                        break
                }
            for (element in this@drop) {
                send(element)
            }
        }
    }

/**
 * Returns a channel containing all elements except first elements that satisfy the given [predicate].
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T> ReceiveChannel<T>.dropWhile(context: CoroutineContext = Unconfined, predicate: suspend (T) -> Boolean): ReceiveChannel<T> =
    produce(context) {
        consume {
            for (element in this@dropWhile) {
                if (!predicate(element))
                    break
            }
            for (element in this@dropWhile) {
                send(element)
            }
        }
    }

/**
 * Returns a channel containing only elements matching the given [predicate].
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T> ReceiveChannel<T>.filter(context: CoroutineContext = Unconfined, predicate: suspend (T) -> Boolean): ReceiveChannel<T> =
    produce(context) {
        consumeEach {
            if (predicate(it)) send(it)
        }
    }

/**
 * Returns a channel containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T> ReceiveChannel<T>.filterIndexed(context: CoroutineContext = Unconfined, predicate: suspend (index: Int, T) -> Boolean): ReceiveChannel<T> =
    produce(context) {
        var index = 0
        consumeEach {
            if (predicate(index++, it)) send(it)
        }
    }

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, C : MutableCollection<in T>> ReceiveChannel<T>.filterIndexedTo(destination: C, predicate: (index: Int, T) -> Boolean): C {
    consumeEachIndexed { (index, element) ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Returns a channel containing all elements not matching the given [predicate].
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T> ReceiveChannel<T>.filterNot(predicate: suspend (T) -> Boolean): ReceiveChannel<T> =
    filter { !predicate(it) }

/**
 * Returns a channel containing all elements that are not `null`.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
@Suppress("UNCHECKED_CAST")
public suspend fun <T : Any> ReceiveChannel<T?>.filterNotNull(): ReceiveChannel<T> =
    filter { it != null } as ReceiveChannel<T>

/**
 * Appends all elements that are not `null` to the given [destination].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <C : MutableCollection<in T>, T : Any> ReceiveChannel<T?>.filterNotNullTo(destination: C): C {
    consumeEach {
        if (it != null) destination.add(it)
    }
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, C : MutableCollection<in T>> ReceiveChannel<T>.filterNotTo(destination: C, predicate: (T) -> Boolean): C {
    consumeEach {
        if (!predicate(it)) destination.add(it)
    }
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, C : MutableCollection<in T>> ReceiveChannel<T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
    consumeEach {
        if (predicate(it)) destination.add(it)
    }
    return destination
}

/**
 * Returns a channel containing first [n] elements.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.take(n: Int, context: CoroutineContext = Unconfined): ReceiveChannel<T> =
    produce(context) {
        consume {
            if (n == 0) return@produce
            require(n >= 0) { "Requested element count $n is less than zero." }
            var remaining: Int = n
            for (element in this@take) {
                send(element)
                remaining--
                if (remaining == 0)
                    return@produce
            }
        }
    }

/**
 * Returns a channel containing first elements satisfying the given [predicate].
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T> ReceiveChannel<T>.takeWhile(context: CoroutineContext = Unconfined, predicate: suspend (T) -> Boolean): ReceiveChannel<T> =
    produce(context) {
        consumeEach {
            if (!predicate(it)) return@produce
            send(it)
        }
    }

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given channel.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, V> ReceiveChannel<T>.associate(transform: (T) -> Pair<K, V>): Map<K, V> =
    associateTo(LinkedHashMap<K, V>(), transform)

/**
 * Returns a [Map] containing the elements from the given channel indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K> ReceiveChannel<T>.associateBy(keySelector: (T) -> K): Map<K, T> =
    associateByTo(LinkedHashMap<K, T>(), keySelector)

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given channel.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, V> ReceiveChannel<T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, V> =
    associateByTo(LinkedHashMap<K, V>(), keySelector, valueTransform)

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given channel
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, M : MutableMap<in K, in T>> ReceiveChannel<T>.associateByTo(destination: M, keySelector: (T) -> K): M {
    consumeEach {
        destination.put(keySelector(it), it)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given channel.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, V, M : MutableMap<in K, in V>> ReceiveChannel<T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    consumeEach {
        destination.put(keySelector(it), valueTransform(it))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given channel.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, V, M : MutableMap<in K, in V>> ReceiveChannel<T>.associateTo(destination: M, transform: (T) -> Pair<K, V>): M {
    consumeEach {
        destination += transform(it)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T, C : MutableCollection<in T>> ReceiveChannel<T>.toCollection(destination: C): C {
    consumeEach {
        destination.add(it)
    }
    return destination
}

/**
 * Returns a [List] containing all elements.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.toList(): List<T> =
    this.toMutableList()

/**
 * Returns a [Map] filled with all elements of this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <K, V> ReceiveChannel<Pair<K, V>>.toMap(): Map<K, V> =
    toMap(LinkedHashMap<K, V>())

/**
 * Returns a [MutableMap] filled with all elements of this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <K, V, M : MutableMap<in K, in V>> ReceiveChannel<Pair<K, V>>.toMap(destination: M): M {
    consumeEach {
        destination += it
    }
    return destination
}

/**
 * Returns a [MutableList] filled with all elements of this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.toMutableList(): MutableList<T> =
    toCollection(ArrayList<T>())

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.toSet(): Set<T> =
    toCollection(LinkedHashSet<T>())

/**
 * Returns a single channel of all elements from results of [transform] function being invoked on each element of original channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T, R> ReceiveChannel<T>.flatMap(context: CoroutineContext = Unconfined, transform: suspend (T) -> ReceiveChannel<R>): ReceiveChannel<R> =
    produce(context) {
        consumeEach {
            transform(it).consumeEach {
                send(it)
            }
        }
    }

/**
 * Groups elements of the original channel by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K> ReceiveChannel<T>.groupBy(keySelector: (T) -> K): Map<K, List<T>> =
    groupByTo(LinkedHashMap<K, MutableList<T>>(), keySelector)

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original channel
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, V> ReceiveChannel<T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, List<V>> =
    groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)

/**
 * Groups elements of the original channel by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, M : MutableMap<in K, MutableList<T>>> ReceiveChannel<T>.groupByTo(destination: M, keySelector: (T) -> K): M {
    consumeEach {
        val key = keySelector(it)
        val list = destination.getOrPut(key) { ArrayList<T>() }
        list.add(it)
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original channel
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, K, V, M : MutableMap<in K, MutableList<V>>> ReceiveChannel<T>.groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    consumeEach {
        val key = keySelector(it)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(it))
    }
    return destination
}

/**
 * Returns a channel containing the results of applying the given [transform] function
 * to each element in the original channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T, R> ReceiveChannel<T>.map(context: CoroutineContext = Unconfined, transform: suspend (T) -> R): ReceiveChannel<R> =
    produce(context) {
        consumeEach {
            send(transform(it))
        }
    }

/**
 * Returns a channel containing the results of applying the given [transform] function
 * to each element and its index in the original channel.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T, R> ReceiveChannel<T>.mapIndexed(context: CoroutineContext = Unconfined, transform: suspend (index: Int, T) -> R): ReceiveChannel<R> =
    produce(context) {
        var index = 0
        consumeEach {
            send(transform(index++, it))
        }
    }

/**
 * Returns a channel containing only the non-null results of applying the given [transform] function
 * to each element and its index in the original channel.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T, R : Any> ReceiveChannel<T>.mapIndexedNotNull(context: CoroutineContext = Unconfined, transform: suspend (index: Int, T) -> R?): ReceiveChannel<R> =
    mapIndexed(context, transform).filterNotNull()

/**
 * Applies the given [transform] function to each element and its index in the original channel
 * and appends only the non-null results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R : Any, C : SendChannel<R>> ReceiveChannel<T>.mapIndexedNotNullTo(destination: C, transform: (index: Int, T) -> R?): C {
    consumeEachIndexed { (index, element) ->
        transform(index, element)?.let { destination.send(it) }
    }
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original channel
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R, C : SendChannel<R>> ReceiveChannel<T>.mapIndexedTo(destination: C, transform: (index: Int, T) -> R): C {
    var index = 0
    consumeEach {
        destination.send(transform(index++, it))
    }
    return destination
}

/**
 * Returns a channel containing only the non-null results of applying the given [transform] function
 * to each element in the original channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T, R : Any> ReceiveChannel<T>.mapNotNull(context: CoroutineContext = Unconfined, transform: suspend (T) -> R?): ReceiveChannel<R> =
    map(context, transform).filterNotNull()

/**
 * Applies the given [transform] function to each element in the original channel
 * and appends only the non-null results to the given [destination].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R : Any, C : SendChannel<R>> ReceiveChannel<T>.mapNotNullTo(destination: C, transform: (T) -> R?): C {
    consumeEach {
        transform(it)?.let { destination.send(it) }
    }
    return destination
}

/**
 * Applies the given [transform] function to each element of the original channel
 * and appends the results to the given [destination].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R, C : SendChannel<R>> ReceiveChannel<T>.mapTo(destination: C, transform: (T) -> R): C {
    consumeEach {
        destination.send(transform(it))
    }
    return destination
}

/**
 * Returns a channel of [IndexedValue] for each element of the original channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.withIndex(context: CoroutineContext = Unconfined): ReceiveChannel<IndexedValue<T>> =
    produce(context) {
        var index = 0
        consumeEach {
            send(IndexedValue(index++, it))
        }
    }

/**
 * Returns a channel containing only distinct elements from the given channel.
 *
 * The elements in the resulting channel are in the same order as they were in the source channel.
 *
 * The operation is _intermediate_ and _stateful_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.distinct(): ReceiveChannel<T> =
    this.distinctBy { it }

/**
 * Returns a channel containing only elements from the given channel
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting channel are in the same order as they were in the source channel.
 *
 * The operation is _intermediate_ and _stateful_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
// todo: mark predicate with crossinline modifier when it is supported: https://youtrack.jetbrains.com/issue/KT-19159
public suspend fun <T, K> ReceiveChannel<T>.distinctBy(context: CoroutineContext = Unconfined, selector: suspend (T) -> K): ReceiveChannel<T> =
    produce(context) {
        val keys = HashSet<K>()
        consumeEach {
            val k = selector(it)
            if (k !in keys) {
                send(it)
                keys += k
            }
        }
    }

/**
 * Returns a mutable set containing all distinct elements from the given channel.
 *
 * The returned set preserves the element iteration order of the original channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.toMutableSet(): MutableSet<T> {
    val set = LinkedHashSet<T>()
    consumeEach {
        set.add(it)
    }
    return set
}

/**
 * Returns `true` if all elements match the given [predicate].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.all(predicate: (T) -> Boolean): Boolean {
    consumeEach {
        if (!predicate(it)) return false
    }
    return true
}

/**
 * Returns `true` if channel has at least one element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.any(): Boolean =
    consume {
        return iterator().hasNext()
    }

/**
 * Returns `true` if at least one element matches the given [predicate].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.any(predicate: (T) -> Boolean): Boolean {
    consumeEach {
        if (predicate(it)) return true
    }
    return false
}

/**
 * Returns the number of elements in this channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.count(): Int {
    var count = 0
    consumeEach { count++ }
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.count(predicate: (T) -> Boolean): Int {
    var count = 0
    consumeEach {
        if (predicate(it)) count++
    }
    return count
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R> ReceiveChannel<T>.fold(initial: R, operation: (acc: R, T) -> R): R {
    var accumulator = initial
    consumeEach {
        accumulator = operation(accumulator, it)
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original channel.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R> ReceiveChannel<T>.foldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R): R {
    var index = 0
    var accumulator = initial
    consumeEach {
        accumulator = operation(index++, accumulator, it)
    }
    return accumulator
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R : Comparable<R>> ReceiveChannel<T>.maxBy(selector: (T) -> R): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext()) return null
        var maxElem = iterator.next()
        var maxValue = selector(maxElem)
        while (iterator.hasNext()) {
            val e = iterator.next()
            val v = selector(e)
            if (maxValue < v) {
                maxElem = e
                maxValue = v
            }
        }
        return maxElem
    }

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.maxWith(comparator: Comparator<in T>): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext()) return null
        var max = iterator.next()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (comparator.compare(max, e) < 0) max = e
        }
        return max
    }

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T, R : Comparable<R>> ReceiveChannel<T>.minBy(selector: (T) -> R): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext()) return null
        var minElem = iterator.next()
        var minValue = selector(minElem)
        while (iterator.hasNext()) {
            val e = iterator.next()
            val v = selector(e)
            if (minValue > v) {
                minElem = e
                minValue = v
            }
        }
        return minElem
    }

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.minWith(comparator: Comparator<in T>): T? =
    consume {
        val iterator = iterator()
        if (!iterator.hasNext()) return null
        var min = iterator.next()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (comparator.compare(min, e) > 0) min = e
        }
        return min
    }

/**
 * Returns `true` if the channel has no elements.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T> ReceiveChannel<T>.none(): Boolean =
    consume {
        return !iterator().hasNext()
    }

/**
 * Returns `true` if no elements match the given [predicate].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.none(predicate: (T) -> Boolean): Boolean {
    consumeEach {
        if (predicate(it)) return false
    }
    return true
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <S, T : S> ReceiveChannel<T>.reduce(operation: (acc: S, T) -> S): S =
    consume {
        val iterator = this.iterator()
        if (!iterator.hasNext()) throw UnsupportedOperationException("Empty channel can't be reduced.")
        var accumulator: S = iterator.next()
        while (iterator.hasNext()) {
            accumulator = operation(accumulator, iterator.next())
        }
        return accumulator
    }

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original channel.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <S, T : S> ReceiveChannel<T>.reduceIndexed(operation: (index: Int, acc: S, T) -> S): S =
    consume {
        val iterator = this.iterator()
        if (!iterator.hasNext()) throw UnsupportedOperationException("Empty channel can't be reduced.")
        var index = 1
        var accumulator: S = iterator.next()
        while (iterator.hasNext()) {
            accumulator = operation(index++, accumulator, iterator.next())
        }
        return accumulator
    }

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.sumBy(selector: (T) -> Int): Int {
    var sum: Int = 0
    consumeEach {
        sum += selector(it)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the channel.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.sumByDouble(selector: (T) -> Double): Double {
    var sum: Double = 0.0
    consumeEach {
        sum += selector(it)
    }
    return sum
}

/**
 * Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T : Any> ReceiveChannel<T?>.requireNoNulls(): ReceiveChannel<T> =
    map { it ?: throw IllegalArgumentException("null element found in $this.") }

/**
 * Splits the original channel into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public inline suspend fun <T> ReceiveChannel<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = ArrayList<T>()
    val second = ArrayList<T>()
    consumeEach {
        if (predicate(it)) {
            first.add(it)
        } else {
            second.add(it)
        }
    }
    return Pair(first, second)
}

/**
 * Send each element of the original channel
 * and appends the results to the given [destination].
 *
 * The operation is _terminal_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 */
public suspend fun <T, C : SendChannel<T>> ReceiveChannel<T>.consumeTo(destination: C): C {
    consumeEach {
        destination.send(it)
    }
    return destination
}

/**
 * Returns a channel of pairs built from elements of both channels with same indexes.
 * Resulting channel has length of shortest input channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of both the original [ReceiveChannel] and the `other` one.
 */
public infix suspend fun <T, R> ReceiveChannel<T>.zip(other: ReceiveChannel<R>): ReceiveChannel<Pair<T, R>> =
    zip(other) { t1, t2 -> t1 to t2 }

/**
 * Returns a channel of values built from elements of both collections with same indexes using provided [transform]. Resulting channel has length of shortest input channels.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of both the original [ReceiveChannel] and the `other` one.
 */
public suspend fun <T, R, V> ReceiveChannel<T>.zip(other: ReceiveChannel<R>, context: CoroutineContext = Unconfined, transform: (a: T, b: R) -> V): ReceiveChannel<V> =
    produce(context) {
        other.consume {
            val otherIterator = other.iterator()
            this@zip.consumeEach { element1 ->
                if (!otherIterator.hasNext()) return@consumeEach
                val element2 = otherIterator.next()
                send(transform(element1, element2))
            }
        }
    }
