package world.phantasmal.core

import kotlinx.coroutines.await
import world.phantasmal.core.externals.browser.WritableStream

suspend inline fun <S : WritableStream, R> S.use(block: (S) -> R): R {
    try {
        return block(this)
    } finally {
        close().await()
    }
}
