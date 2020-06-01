package com.remo.bluetoothtestapp.bluetoothsdk.ext

import io.reactivex.*
import io.reactivex.Observable
import com.remo.bluetoothtestapp.bluetoothsdk.rx.TakeWhileInclusive
import java.util.*

//inline fun <T, R> Observable<T>.flatMapSingleSequentially(crossinline mapper: (T) -> SingleSource<R>): Single<List<R>>
//        = map { mapper(it) }
//        .toList()
//        .flatMap { operations: List<SingleSource<out R>> ->
//            val results = ArrayList<R>()
//            var sequence = Single.just(results)
//            operations.forEach { op: SingleSource<out R> ->
//                sequence = sequence.flatMap { op }
//                        .map {
//                            results.add(it)
//                            results
//                        }
//            }
//            sequence
//        }

fun <T, R> Observable<T>.flatMapSingleSequentially(mapper: (T) -> SingleSource<R>): Single<List<R>>
        = flatMapSingleConcurrently(mapper, 1)

fun <T, R> Single<out Iterable<T>>.flatMapSequentially(mapper: (T) -> SingleSource<R>): Single<List<R>>
        = flatMapConcurrently(mapper, 1)

fun <T, R> Single<out Iterator<T>>.flatMapSequentiallyOnIterator(mapper: (T) -> SingleSource<R>): Single<List<R>>
        = flatMapConcurrentlyOnIterator(mapper, 1)

fun <T, R> Observable<T>.flatMapSingleConcurrently(mapper: (T) -> SingleSource<R>, concurrently: Int): Single<List<R>>
        = toList()
        .flatMapConcurrently(mapper, concurrently)

fun <T, R> Single<out Iterable<T>>.flatMapConcurrently(mapper: (T) -> SingleSource<R>, concurrently: Int): Single<List<R>>
        = map { it.iterator() }
        .flatMapConcurrentlyOnIterator(mapper, concurrently)

fun <T, R> Single<out Iterator<T>>.flatMapConcurrentlyOnIterator(mapper: (T) -> SingleSource<R>, concurrently: Int): Single<List<R>>
        = map { ArrayList<SingleSource<R>>() to it }
        .map { pair ->
            @Suppress("NAME_SHADOWING")
            val concurrently = Math.max(concurrently, 1)
            while (pair.first.size < concurrently && pair.second.hasNext())
                pair.first.add(pair.second.next().let(mapper))
            pair
        }
        .flatMap { pair ->
            val results = LinkedList<R>()
            var sequence = Single.just(results)

            pair.first.forEach { op: SingleSource<out R> ->
                sequence = sequence.flatMap { op }
                        .map {
                            results.add(it)
                            results
                        }
            }

            if (pair.second.hasNext()) {
                sequence = sequence.map { pair.second }
                        .flatMapConcurrentlyOnIterator(mapper, concurrently)
                        .map {
                            results.addAll(it)
                            results
                        }
            }

            sequence
        }

//--------------------------------------------------------------------------------------------------

fun <T> Observable<T>?.orEmpty(): Observable<T> = this ?: Observable.empty()

fun <T> Observable<T?>.filterNotNull(): Observable<T> = this.filter { it != null }.map { it!! }

fun <T> Observable<T>.first(predicate: (T) -> Boolean, defaultItem: T): Single<T> = this.filter(predicate).first(defaultItem)

fun <T> Observable<T>.firstOrError(predicate: (T) -> Boolean): Single<T> = this.filter(predicate).firstOrError()

fun <T> Observable<T>.firstElement(predicate: (T) -> Boolean): Maybe<T> = this.filter(predicate).firstElement()

fun <T> Observable<T>.takeWhileInclusive(predicate: (T) -> Boolean): Observable<T> = TakeWhileInclusive(predicate).apply(this)

fun <T> Flowable<T>.takeWhileInclusive(predicate: (T) -> Boolean): Flowable<T> = TakeWhileInclusive(predicate).apply(this)
