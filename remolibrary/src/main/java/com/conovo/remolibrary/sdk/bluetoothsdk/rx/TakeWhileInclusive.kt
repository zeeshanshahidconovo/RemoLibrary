package com.remo.bluetoothtestapp.bluetoothsdk.rx

import io.reactivex.*

class TakeWhileInclusive<T>(
        private val predicate: (T) -> Boolean
) : ObservableTransformer<T, T>, FlowableTransformer<T, T> {

    override fun apply(upstream: Observable<T>): Observable<T> = ObservableTakeWhileInclusive(predicate, upstream)

    override fun apply(upstream: Flowable<T>): Flowable<T> = FlowableTakeWhileInclusive(predicate, upstream)
}
