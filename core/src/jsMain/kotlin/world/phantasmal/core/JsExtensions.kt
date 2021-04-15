package world.phantasmal.core

import world.phantasmal.core.externals.browser.WritableStream

inline fun <S : WritableStream, R> S.use(block: (S) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}
