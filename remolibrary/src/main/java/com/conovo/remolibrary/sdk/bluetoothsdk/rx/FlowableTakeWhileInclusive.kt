package com.remo.bluetoothtestapp.bluetoothsdk.rx

import io.reactivex.Flowable
import io.reactivex.FlowableSubscriber
import io.reactivex.exceptions.Exceptions
import io.reactivex.internal.subscriptions.SubscriptionHelper
import io.reactivex.plugins.RxJavaPlugins
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class FlowableTakeWhileInclusive<T>(
        private val predicate: (T) -> Boolean,
        private val upstream: Flowable<T>
) : Flowable<T>() {

    override fun subscribeActual(s: Subscriber<in T>) {
        upstream.subscribe(TakeWhileInclusiveSubscriber<T>(predicate, s))
    }

    class TakeWhileInclusiveSubscriber<T>(
            private val predicate: (T) -> Boolean,
            private val downstream: Subscriber<in T>
    ) : FlowableSubscriber<T>, Subscription {

        private var upstream: Subscription? = null

        private var done: Boolean = false

        override fun onSubscribe(s: Subscription) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s
                downstream.onSubscribe(this)
            }
        }

        override fun onNext(t: T) {
            if (done) {
                return
            }
            val b: Boolean
            try {
                b = predicate(t)
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                upstream?.cancel()
                onError(e)
                return
            }

            downstream.onNext(t)

            if (!b) {
                done = true
                upstream?.cancel()
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

        override fun request(n: Long) {
            upstream?.request(n)
        }

        override fun cancel() {
            upstream?.cancel()
        }
    }
}
