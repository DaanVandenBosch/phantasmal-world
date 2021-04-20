package world.phantasmal.web.shared.logging

import mu.Appender

class LogAppender : Appender {
    override fun trace(message: Any?) {
        if (message is MessageWithThrowable) {
            console.log(message.message, message.throwable)
        } else {
            console.log(message)
        }
    }

    override fun debug(message: Any?) {
        if (message is MessageWithThrowable) {
            console.log(message.message, message.throwable)
        } else {
            console.log(message)
        }
    }

    override fun info(message: Any?) {
        if (message is MessageWithThrowable) {
            console.info(message.message, message.throwable)
        } else {
            console.info(message)
        }
    }

    override fun warn(message: Any?) {
        if (message is MessageWithThrowable) {
            console.warn(message.message, message.throwable)
        } else {
            console.warn(message)
        }
    }

    override fun error(message: Any?) {
        if (message is MessageWithThrowable) {
            console.error(message.message, message.throwable)
        } else {
            console.error(message)
        }
    }
}
