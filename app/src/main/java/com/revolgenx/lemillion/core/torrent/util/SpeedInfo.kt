package com.revolgenx.lemillion.core.torrent.util

class SpeedInfo {

    var samples = mutableListOf<Sample>()
    @get:Synchronized

    var peak: Long = 0

    // sampleStart sample use to calculate average speed
    private var sampleStart: Sample? = null

    /**
     * Current download speed
     *
     * @return bytes per second
     */
    // [s1] [s2] [EOF]
    val currentSpeed: Int
        @Synchronized get() {
            if (rowSamples < 2)
                return 0
            val s1 = samples[samples.size - 2]
            val s2 = samples[samples.size - 1]

            val current = s2.current - s1.current
            val time = s2.now - s1.now

            return if (time == 0L) 0 else (current * 1000 / time).toInt()

        }

    /**
     * Average speed from sampleStart download
     *
     * @return bytes per second
     */
    val averageSpeed: Int
        @Synchronized get() {
            if (sampleStart == null || rowSamples < 2)
                return 0

            val s2 = samples[samples.size - 1]

            val current = s2.current - sampleStart!!.current
            val time = s2.now - sampleStart!!.now

            return (current * 1000 / time).toInt()
        }

    /**
     * Number of samples
     *
     * @return return number of samples in the row (before download restart)
     */
    protected val rowSamples: Int
        get() {
            for (i in samples.indices.reversed()) {
                val s = samples[i]
                if (s.start)
                    return samples.size - i
            }

            return samples.size
        }

    protected val lastUpdate: Long
        get() {
            if (samples.size == 0)
                return 0
            val s = samples[samples.size - 1]
            return s.now
        }

    inner class Sample {
        // bytes downloaded
        var current: Long = 0
        // current time
        var now: Long = 0
        // sampleStart block? used to mark block after download has been altered / restarted
        var start: Boolean = false

        constructor() {
            current = 0
            now = System.currentTimeMillis()
            start = false
        }

        constructor(current: Long) {
            this.current = current
            now = System.currentTimeMillis()
            start = false
        }

        constructor(current: Long, now: Long) {
            this.current = current
            this.now = now
            start = false
        }
    }

    /**
     * Start calculate speed from 'current' bytes downloaded
     *
     * @param current
     * current length
     */
    @Synchronized
    fun start(current: Long) {
        val s = Sample(current)
        s.start = true
        sampleStart = s
        add(s)
    }

    /**
     * step download process with 'current' bytes downloaded
     *
     * @param current
     * current length
     */
    @Synchronized
    fun step(current: Long) {
        val now = System.currentTimeMillis()

        val lastUpdate = lastUpdate
        if (lastUpdate + SAMPLE_LENGTH < now)
            add(Sample(current, now))
    }

    @Synchronized
    fun end(current: Long) {
        val now = System.currentTimeMillis()

        var lastUpdate: Long = 0
        var lastCurrent: Long = 0

        if (samples.size > 0) {
            val s = samples[samples.size - 1]
            lastUpdate = s.now
            lastCurrent = s.current
        }

        // step() may be at exact time or position with end(). skip it then.
        if (lastUpdate < now && lastCurrent < current)
            add(Sample(current, now))
    }

    /**
     * Average speed for maximum stepsBack steps
     *
     * @param stepsBack
     * how many steps aproximate
     * @return bytes per second
     */
    @Synchronized
    fun getAverageSpeed(stepsBack: Int): Int {
        if (sampleStart == null || rowSamples < 2)
            return 0

        val is2 = samples.size - 1
        var is1 = is2 - stepsBack
        if (is1 < 0)
            is1 = 0

        var s1 = samples[is1]

        // if steps back below sampleStart download, then use sampleStart mark
        if (s1.now < sampleStart!!.now)
            s1 = sampleStart!!

        val s2 = samples[is2]

        val current = s2.current - s1.current
        val time = s2.now - s1.now

        return (current * 1000 / time).toInt()
    }

    @Synchronized
    fun getSamples(): Int {
        return samples.size
    }

    @Synchronized
    fun getSample(index: Int): Sample {
        return samples[index]
    }

    //
    // protected
    //

    protected fun getStart(): Sample? {
        for (i in samples.indices.reversed()) {
            val s = samples[i]
            if (s.start)
                return s
        }

        return null
    }

    protected fun add(s: Sample) {
        // check if we have broken / restarted download. check if here some samples
        if (samples.size > 0) {
            val s1 = samples[samples.size - 1]
            // check if last download 'current' stands before current 'current' download
            if (s1.current > s.current) {
                s.start = true
                sampleStart = s
            }
        }

        samples.add(s)

        while (samples.size > SAMPLE_MAX)
            samples.removeAt(0)

        peakUpdate()
    }

    protected fun peakUpdate() {
        peak = 0
        for (s in samples) {
            if (peak < s.current)
                peak = s.current
        }
    }

    companion object {
        var SAMPLE_LENGTH = 1000
        var SAMPLE_MAX = 20
    }
}
