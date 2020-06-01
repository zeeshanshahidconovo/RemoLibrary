package com.remo.bluetoothtestapp.bluetoothsdk.ext

import io.reactivex.Flowable
import io.reactivex.Single
import com.remo.bluetoothtestapp.bluetoothsdk.model.Sample
import com.remo.bluetoothtestapp.bluetoothsdk.util.RemoUtil
import java.io.File
import java.io.FileDescriptor
import java.io.FileWriter
import java.util.*
import java.util.concurrent.TimeUnit

fun Flowable<Sample>.log(fileWriterSupplier: () -> FileWriter, sampleToLogEntry: (Sample) -> String = Sample::toCSV): Flowable<Sample>
        = compose(RemoUtil.logTransformer(fileWriterSupplier, sampleToLogEntry))

fun Flowable<Sample>.log(fileName: String, sampleToLogEntry: (Sample) -> String = Sample::toCSV): Flowable<Sample>
        = compose(RemoUtil.logTransformer(fileName, sampleToLogEntry))

fun Flowable<Sample>.log(fileName: String, append: Boolean, sampleToLogEntry: (Sample) -> String = Sample::toCSV): Flowable<Sample>
        = compose(RemoUtil.logTransformer(fileName, append, sampleToLogEntry))

fun Flowable<Sample>.log(file: File, sampleToLogEntry: (Sample) -> String = Sample::toCSV): Flowable<Sample>
        = compose(RemoUtil.logTransformer(file, sampleToLogEntry))

fun Flowable<Sample>.log(file: File, append: Boolean, sampleToLogEntry: (Sample) -> String = Sample::toCSV): Flowable<Sample>
        = compose(RemoUtil.logTransformer(file, append, sampleToLogEntry))

fun Flowable<Sample>.log(fd: FileDescriptor, sampleToLogEntry: (Sample) -> String = Sample::toCSV): Flowable<Sample>
        = compose(RemoUtil.logTransformer(fd, sampleToLogEntry))

fun Flowable<Sample>.bufferSamples(timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Single<out List<Sample>> =
        scan(LinkedList<Sample>()) { list, sample ->
            list.addLast(sample)
            list
        }
                .filter { samples -> samples.size > 1 } // We need at least 2 samples to calculate time window
                .takeWhile { samples -> samples.isTimeWindowValid(timespan, unit) }
                .lastOrError() // We take the biggest (the last) list with a correct time window

fun Collection<Sample>.isTimeWindowValid(timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Boolean
        = RemoUtil.isTimeWindowValid(this, timespan, unit)

fun Sample.isTimeWindowValid(to: Sample, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Boolean
        = RemoUtil.isTimeWindowValid(this, to, timespan, unit)

fun Iterable<Sample>.calculateBaseline(): Double
        = RemoUtil.calculateBaseline(this)

fun Flowable<Sample>.baseline(timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Single<Double>
        = bufferSamples(timespan, unit).map { samples -> samples.calculateBaseline() }

fun Flowable<Sample>.baselineContinuous(timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Flowable<Double>
        = compose(RemoUtil.baselineTransformer(timespan, unit))

fun Iterable<Sample>.calculateEmgOffset(): List<Double>
        = RemoUtil.calculateEmgOffset(this)

fun Flowable<Sample>.emgOffset(timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Single<List<Double>>
        = bufferSamples(timespan, unit).map { samples -> samples.calculateEmgOffset() }

fun Flowable<Sample>.emgOffsetContinuous(timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Flowable<List<Double>>
        = compose(RemoUtil.emgOffsetTransformer(timespan, unit))

fun Iterable<Sample>.calculateEmgRatio(baseline: Double): Double
        = RemoUtil.calculateEmgRatio(this, baseline)

fun Flowable<Sample>.emgRatio(baseline: Double, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Single<Double>
        = bufferSamples(timespan, unit).map { samples -> samples.calculateEmgRatio(baseline) }

fun Flowable<Sample>.emgRatioContinuous(baseline: Double, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Flowable<Double>
        = compose(RemoUtil.emgRatioTransformer(baseline, timespan, unit))

fun Iterable<Sample>.calculateMostActiveChannel(emgOffset: Iterable<Double>): Int?
        = RemoUtil.calculateMostActiveChannel(this, emgOffset)

fun Flowable<Sample>.mostActiveChannel(emgOffset: Iterable<Double>, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Single<Int?>
        = bufferSamples(timespan, unit).map { samples -> samples.calculateMostActiveChannel(emgOffset) }

fun Flowable<Sample>.mostActiveChannelContinuous(emgOffset: Iterable<Double>, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Flowable<Int?>
        = compose(RemoUtil.mostActiveChannelTransformer(emgOffset, timespan, unit))

fun Iterable<Sample>.calculateNormalizationValue(emgOffset: Iterable<Double>): Double?
        = RemoUtil.calculateNormalizationValue(this, emgOffset)

fun Flowable<Sample>.normalizationValue(emgOffset: Iterable<Double>, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Single<Double?>
        = bufferSamples(timespan, unit).map { samples -> samples.calculateNormalizationValue(emgOffset) }

fun Flowable<Sample>.normalizationValueContinuous(emgOffset: Iterable<Double>, timespan: Long = RemoUtil.DEFAULT_TIMESPAN, unit: TimeUnit = RemoUtil.DEFAULT_TIMEUNIT): Flowable<Double?>
        = compose(RemoUtil.normalizationValueTransformer(emgOffset, timespan, unit))

fun Sample.calculateIntensity(emgOffset: List<Double>, mostActiveChannel: Int, normalizationValue: Double): Double
        = RemoUtil.calculateIntensity(this, emgOffset, mostActiveChannel, normalizationValue)

fun Sample.calculateIntensities(emgOffset: List<Double>, mostActiveChannels: Sequence<Int>, normalizationValues: Sequence<Double>): List<Double>
        = RemoUtil.calculateIntensities(this, emgOffset, mostActiveChannels, normalizationValues)

fun Sample.calculateIntensities(emgOffset: List<Double>, mostActiveChannels: Iterable<Int>, normalizationValues: Iterable<Double>): List<Double>
        = RemoUtil.calculateIntensities(this, emgOffset, mostActiveChannels, normalizationValues)

fun Flowable<Sample>.intensitiesContinuous(emgOffset: List<Double>, mostActiveChannels: Iterable<Int>, normalizationValues: Iterable<Double>): Flowable<List<Double>>
        = compose(RemoUtil.intensitiesTransformer(emgOffset, mostActiveChannels, normalizationValues))

fun Flowable<List<Double>>.lowPassContinuous(samplesCount: Int = RemoUtil.DEFAULT_LOW_PASS_SAMPLES_COUNT): Flowable<List<Double>>
        = compose(RemoUtil.lowPassTransformer(samplesCount))
