package com.complexly.academy.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.complexly.academy.ScienceApp
import kotlinx.coroutines.CoroutineScope
import java.io.File

actual fun createPlatformDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        produceFile = {
            ScienceApp.instance.filesDir.resolve("academy.preferences_pb")
        }
    )
}

actual fun createTestDataStore(name: String, scope: CoroutineScope): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = {
            File.createTempFile(name, ".preferences_pb")
        }
    )
}
