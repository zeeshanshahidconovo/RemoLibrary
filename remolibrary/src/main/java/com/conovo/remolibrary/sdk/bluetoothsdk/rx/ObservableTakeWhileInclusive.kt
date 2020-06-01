package com.remo.bluetoothtestapp.bluetoothsdk.rx

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.Exceptions
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.plugins.RxJavaPlugins

class ObservableTakeWhileInclusive<T>(
        private val predicate: (T) -> Boolean,
        private val upstream: Observable<T>
) : Observable<T>() {

    override fun subscribeActual(observer: Observer<in T>) {
        upstream.subscribe(TakeWhileInclusiveObserver(predicate, observer))
    }

    class TakeWhileInclusiveObserver<T>(
            private val predicate: (T) -> Boolean,
            private val downstream: Observer<in T>
    ) : Observer<T>, Disposable {

        private var upstream: Disposable? = null

        private var done: Boolean = false

        override fun onSubscribe(d: Disposable) {
            if (DisposableHelper.validate(upstream, d)) {
                upstream = d
                downstream.onSubscribe(this)
            }
        }

        override fun dispose() {
            upstream?.dispose()
        }

        override fun isDisposed(): Boolean = upstream?.isDisposed ?: false

        override fun onNext(t: T) {
            if (done) {
                return
            }
            val b: Boolean
            try {
                b = predicate(t)
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                upstream?.dispose()
                onError(e)
                return
            }

            downstream.onNext(t)

            if (!b) {
                done = true
                upstream?.dispose()
                downstream.onComplete()
            }
        }

        override fun onError(t: Throwable) {
            if (done) {
                RxJavaPlugins.onError(t)
                return
            }
            done = true
            downstream.onError(t)
        }

        override fun onComplete() {
            if (done) {
                return
            }
            done = true
            downstream.onComplete()
        }
    }
}
