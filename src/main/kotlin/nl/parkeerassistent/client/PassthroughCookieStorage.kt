package nl.parkeerassistent.client

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PassthroughCookieStorage(session: String?) : CookiesStorage {

    private val container: MutableSet<Cookie> = ConcurrentSet()
    private val mutex = Mutex()
    private var clearCookies = false
    private var hasChange = false

    init {
        session?.let {
            container.addAll(session.split("$").mapNotNull {
                runCatching { parseServerSetCookieHeader(it) }.getOrNull()
            })
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        return@withLock container.toList()
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = mutex.withLock {
        with(cookie) {
            if (name.isBlank()) return@withLock
        }
        if (container.find { it.name == cookie.name && it.value == cookie.value } != null) {
            return@withLock
        }
        container.removeAll { it.name == cookie.name }
        container.add(cookie)
        hasChange = true
    }

    override fun close() {
    }

    fun clear() {
        clearCookies = true
    }

    suspend fun createSetCookie(): String? = mutex.withLock {
        if (clearCookies) {
            return@withLock ""
        }
        if (! hasChange) {
            return@withLock null
        }
        return@withLock container.joinToString(
            separator = "$",
            transform = {
                renderSetCookieHeader(it)
            }
        )
    }

}
