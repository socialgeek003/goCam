package com.lib.gocam.timer

/**
 * Created by Sairaj Gawde on 19-06-2023.
 */
interface CustomCountTimerListener {
    /**
     * Method to be called every second by the [SonicCountDownTimer]
     *
     * @param timeRemaining: Time remaining in milliseconds.
     */
    fun onTimerTick(timeRemaining: Long)

    /**
     * Method to be called by [SonicCountDownTimer] when the thread is getting  finished
     */
    fun onTimerFinish()
}