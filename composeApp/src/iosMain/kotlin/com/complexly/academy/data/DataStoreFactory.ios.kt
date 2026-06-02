package com.complexly.academy.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun createPlatformDataStore(): DataStore<Preferences> {
    val documentDirectory: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!
    return PreferenceDataStoreFactory.createWithPath(
        producePath = { (documentDirectory.path + "/academy.preferences_pb").toPath() }
    )
}

actual fun createTestDataStore(name: String, scope: CoroutineScope): DataStore<Preferences> {
    val documentDirectory: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!
    return PreferenceDataStoreFactory.createWithPath(
        scope = scope,
        producePath = { (documentDirectory.path + "/${name}_test.preferences_pb").toPath() }
    )
}
