package auszug.web

import java.io.IOException

class UncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e !is IOException || e.message != "Connection reset by peer") {
            throw e!!
        }
    }
}