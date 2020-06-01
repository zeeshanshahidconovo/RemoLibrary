package com.remo.bluetoothtestapp.bluetoothsdk.util

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import com.remo.bluetoothtestapp.bluetoothsdk.ext.transpose
import com.remo.bluetoothtestapp.bluetoothsdk.model.Sample
import java.io.File
import java.io.FileDescriptor
import java.io.FileWriter
import java.util.*
import java.util.concurrent.TimeUnit

class RemoUtil {

    companion object {

        val DEFAULT_TIMESPAN: Long = 3
        val DEFAULT_TIMEUNIT: TimeUnit = TimeUnit.SECONDS
        val DEFAULT_LOW_PASS_SAMPLES_COUNT: Int = 5

        private fun fileWriterSupplier(fileName: String): () -> FileWriter
                = { FileWriter(fileName) }

        private fun fileWriterSupplier(fileName: String, append: Boolean): () -> FileWriter
                = { FileWriter(fileName, append) }

        private fun fileWriterSupplier(file: File): () -> FileWriter
                = { FileWriter(file) }

        private fun fileWriterSupplier(file: File, append: Boolean): () -> FileWriter
                = { FileWriter(file, append) }

        private fun fileWriterSupplier(fd: FileDescriptor): () -> FileWriter
                = { FileWriter(fd) }

        fun logTransformer(fileWriterSupplier: () -> FileWriter, sampleToLogEntry: (Sample) -> String = Sample::toCSV): FlowableTransformer<Sample, Sample> =
                FlowableTransformer { upstream ->
                    Flowable.using(fileWriterSupplier, { writer ->
                        upstream.doOnNext { sample ->
                            writer.appendln(sampleToLogEntry(sample))
                            writer.flush()
                        }
                    }, { writer ->
                        writer.close()
                    })
                }

        fun logTransformer(fileName: String, sampleToLogEntry: (Sample) -> String = Sample::toCSV): FlowableTransformer<Sample, Sample>
                = logTransformer(fileWriterSupplier(fileName), sampleToLogEntry)

        fun logTransformer(fileName: String, append: Boolean, sampleToLogEntry: (Sample) -> String = Sample::toCSV): FlowableTransformer<Sample, Sample>
                = logTransformer(fileWriterSupplier(fileName, append), sampleToLogEntry)

        fun logTransformer(file: File, sampleToLogEntry: (Sample) -> String = Sample::toCSV): FlowableTransformer<Sample, Sample>
                = logTransformer(fileWriterSupplier(file), sampleToLogEntry)

        fun logTransformer(file: File, append: Boolean, sampleToLogEntry: (Sample) -> String = Sample::toCSV): FlowableTransformer<Sample, Sample>
                = logTransformer(fileWriterSupplier(file, append), sampleToLogEntry)

        fun logTransformer(fd: FileDescriptor, sampleToLogEntry: (Sample) -> String = Sample::toCSV): FlowableTransformer<Sample, Sample>
                = logTransformer(fileWriterSupplier(fd), sampleToLogEntry)

        fun isTimeWindowValid(samples: Collection<Sample>, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): Boolean
                = samples.isNotEmpty() && isTimeWindowValid(samples.first(), samples.last(), timespan, unit)

        fun isTimeWindowValid(first: Sample, last: Sample, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): Boolean
                = isTimeWindowValid(first.date, last.date, timespan, unit)

        fun isTimeWindowValid(firstMillis: Long, lastMillis: Long, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): Boolean
                = isTimeWindowValid(lastMillis - firstMillis, timespan, unit)

        fun isTimeWindowValid(deltaMillis: Long, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): Boolean
                = isTimeWindowValid(deltaMillis, TimeUnit.MILLISECONDS, timespan, unit)

        fun isTimeWindowValid(delta: Long, deltaUnit: TimeUnit, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): Boolean
                = isTimeWindowValid(deltaUnit.toMillis(delta), unit.toMillis(timespan))

        fun isTimeWindowValid(delta: Long, timespan: Long): Boolean
                = delta < timespan

        fun calculateBaseline(samples: Iterable<Sample>): Double
                = samples.asSequence().map { sample -> sample.measure.sum() }.average()

        fun baselineTransformer(timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): FlowableTransformer<Sample, Double> =
                FlowableTransformer { upstream ->
                    val samples: Queue<Sample> = LinkedList()
                    val partialSums: Queue<Int> = LinkedList()
                    var totalSum: Long = 0

                    upstream.map { sample ->
                        if (samples.offer(sample)) {
                            val emgSum = sample.measure.sum()
                            if (partialSums.offer(emgSum)) {
                                totalSum += emgSum
                            }
                        }

                        while (samples.size > 1 && isTimeWindowValid(samples, timespan, unit).not()) {
                            samples.poll()
                            partialSums.poll()
                                    ?.also { oldEmgSum ->
                                        totalSum -= oldEmgSum
                                    }
                        }

                        val count = partialSums.size
                        val average = if (count == 0) Double.NaN else totalSum.toDouble() / count
                        average
                    }
                }

        fun calculateEmgOffset(samples: Iterable<Sample>): List<Double> =
                samples.asSequence().map { sample -> sample.measure }.transpose()
                        .map { channel -> channel.average() }
                        .toList()

        fun emgOffsetTransformer(timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): FlowableTransformer<Sample, List<Double>> =
                FlowableTransformer { upstream ->
                    val samples: Queue<Sample> = LinkedList()
                    val totalSums: MutableList<Int> = ArrayList()

                    upstream.map { sample ->
                        if (samples.offer(sample)) {
                            sample.measure.forEachIndexed { index, value ->
                                while (index >= totalSums.size)
                                    totalSums.add(0)
                                totalSums[index] = totalSums[index] + value
                            }
                        }

                        while (samples.size > 1 && isTimeWindowValid(samples, timespan, unit).not()) {
                            samples.poll()
                                    ?.also { oldSample ->
                                        oldSample.measure.forEachIndexed { index, value ->
                                            totalSums[index] = totalSums[index] - value
                                        }
                                    }
                        }

                        val count = samples.size
                        totalSums.map { totalSum ->
                            val average = if (count == 0) Double.NaN else totalSum.toDouble() / count
                            average
                        }
                    }
                }

        fun calculateEmgRatio(samples: Iterable<Sample>, baseline: Double): Double
                = calculateBaseline(samples) / baseline

        fun emgRatioTransformer(baseline: Double, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): FlowableTransformer<Sample, Double> =
                FlowableTransformer { upstream ->
                    upstream.compose(baselineTransformer(timespan, unit)).map { it / baseline }
                }

        private fun mostActiveChannelPrivate(emgMean: Sequence<Double>, emgOffset: Sequence<Double>): Int?
                = emgMean.zip(emgOffset).map { it.first - it.second }.withIndex().maxBy { it.value }?.index

        private fun mostActiveChannelPrivate(emgMean: Iterable<Double>, emgOffset: Iterable<Double>): Int?
                = mostActiveChannelPrivate(emgMean.asSequence(), emgOffset.asSequence())

        fun calculateMostActiveChannel(samples: Iterable<Sample>, emgOffset: Iterable<Double>): Int?
                = mostActiveChannelPrivate(calculateEmgOffset(samples), emgOffset)

        fun mostActiveChannelTransformer(emgOffset: Iterable<Double>, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): FlowableTransformer<Sample, Int?> =
                FlowableTransformer { upstream ->
                    upstream.compose(emgOffsetTransformer(timespan, unit)).map { mostActiveChannelPrivate(it, emgOffset) }
                }

        private fun normalizationValuePrivate(emgMean: Sequence<Double>, emgOffset: Sequence<Double>): Double?
                = emgMean.zip(emgOffset).map { it.first - it.second }.max()

        private fun normalizationValuePrivate(emgMean: Iterable<Double>, emgOffset: Iterable<Double>): Double?
                = normalizationValuePrivate(emgMean.asSequence(), emgOffset.asSequence())

        fun calculateNormalizationValue(samples: Iterable<Sample>, emgOffset: Iterable<Double>): Double?
                = normalizationValuePrivate(calculateEmgOffset(samples), emgOffset)

        fun normalizationValueTransformer(emgOffset: Iterable<Double>, timespan: Long = DEFAULT_TIMESPAN, unit: TimeUnit = DEFAULT_TIMEUNIT): FlowableTransformer<Sample, Double?> =
                FlowableTransformer { upstream ->
                    upstream.compose(emgOffsetTransformer(timespan, unit)).map { normalizationValuePrivate(it, emgOffset) }
                }

        fun calculateIntensity(sample: Sample, emgOffset: List<Double>, mostActiveChannel: Int, normalizationValue: Double): Double
                = (sample.measure[mostActiveChannel] - emgOffset[mostActiveChannel]) / normalizationValue

        fun calculateIntensities(sample: Sample, emgOffset: List<Double>, mostActiveChannels: Sequence<Int>, normalizationValues: Sequence<Double>): List<Double>
                = mostActiveChannels.zip(normalizationValues).map { calculateIntensity(sample, emgOffset, it.first, it.second) }.toList()

        fun calculateIntensities(sample: Sample, emgOffset: List<Double>, mostActiveChannels: Iterable<Int>, normalizationValues: Iterable<Double>): List<Double>
                = calculateIntensities(sample, emgOffset, mostActiveChannels.asSequence(), normalizationValues.asSequence())

        fun intensitiesTransformer(emgOffset: List<Double>, mostActiveChannels: Iterable<Int>, normalizationValues: Iterable<Double>): FlowableTransformer<Sample, List<Double>> =
                FlowableTransformer { upstream ->
                    upstream.map { sample -> calculateIntensities(sample, emgOffset, mostActiveChannels, normalizationValues) }
                }

        fun lowPassTransformer(samplesCount: Int = DEFAULT_LOW_PASS_SAMPLES_COUNT): FlowableTransformer<List<Double>, List<Double>> =
                FlowableTransformer { upstream ->
                    upstream.buffer(samplesCount, 1).map { samples ->
                        samples.asSequence().transpose()
                                .map { movement -> movement.average() }
                                .toList()
                    }
                }
    }
}
