package com.jan.food.data.dataSource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * [SecureStore] backed by the iOS Keychain.
 *
 * Each [key] is a generic-password item under a fixed service; the Keychain encrypts at rest, so
 * there's no manual crypto here. The Keychain has no change-notification API, so [observe]
 * synthesizes reactivity: it reads the current value, then re-reads whenever [put]/[remove] signals
 * a change for that key. The app is the only writer, so nothing is missed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSecureStore : SecureStore {

    private val changes = MutableSharedFlow<String>(extraBufferCapacity = 64)

    override suspend fun put(key: String, value: String) {
        write(key, value)
        changes.tryEmit(key)
    }

    override suspend fun remove(key: String) {
        delete(key)
        changes.tryEmit(key)
    }

    override fun observe(key: String): Flow<String?> = flow {
        emit(read(key))
        changes.filter { it == key }.collect { emit(read(key)) }
    }

    private fun write(key: String, value: String) {
        delete(key) // SecItemAdd rejects duplicates, so replace.
        withBaseQuery(key) { query ->
            val cfData = CFBridgingRetain((value as NSString).dataUsingEncoding(NSUTF8StringEncoding))
            CFDictionaryAddValue(query, kSecValueData, cfData)
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            try {
                SecItemAdd(query, null)
            } finally {
                CFRelease(cfData)
            }
        }
    }

    private fun delete(key: String) {
        withBaseQuery(key) { query ->
            SecItemDelete(query)
        }
    }

    private fun read(key: String): String? = withBaseQuery(key) { query ->
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@withBaseQuery null
            val data = CFBridgingRelease(result.value) as? NSData ?: return@withBaseQuery null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
        }
    }

    /** Builds a query dict with class + service + account, releasing the bridged refs afterward. */
    private inline fun <T> withBaseQuery(account: String, block: (CFMutableDictionaryRef?) -> T): T {
        val cfService = CFBridgingRetain(SERVICE as NSString)
        val cfAccount = CFBridgingRetain(account as NSString)
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfService)
        CFDictionaryAddValue(query, kSecAttrAccount, cfAccount)
        try {
            return block(query)
        } finally {
            CFRelease(query)
            CFRelease(cfService)
            CFRelease(cfAccount)
        }
    }

    private companion object {
        const val SERVICE = "com.jan.food.securestore"
    }
}
