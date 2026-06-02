package com.complexly.academy.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope

expect fun createPlatformDataStore(): DataStore<Preferences>

expect fun createTestDataStore(name: String, scope: CoroutineScope): DataStore<Preferences>
