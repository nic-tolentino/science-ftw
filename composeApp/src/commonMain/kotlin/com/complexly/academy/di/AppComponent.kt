package com.complexly.academy.di

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import com.complexly.academy.data.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

@Scope
annotation class AppScope

@Component
@AppScope
abstract class AppComponent {
    abstract val contentRepository: ContentRepository
    abstract val userRepository: UserRepository
    abstract val getRecentlyViewedUseCase: com.complexly.academy.domain.GetRecentlyViewedUseCase

    @Provides
    @AppScope
    fun provideDataStore(): DataStore<Preferences> = createPlatformDataStore()

    @Provides
    @AppScope
    fun provideJsonDataSource(): JsonDataSource = JsonDataSourceImpl()

    @Provides
    @AppScope
    fun provideContentRepository(dataSource: JsonDataSource): ContentRepository =
        ContentRepositoryImpl(dataSource)

    @Provides
    @AppScope
    fun provideUserRepository(
        dataSource: JsonDataSource,
        dataStore: DataStore<Preferences>
    ): UserRepository = UserRepositoryImpl(dataSource, dataStore)

    @Provides
    @AppScope
    fun provideGetRecentlyViewedUseCase(
        contentRepository: ContentRepository,
        userRepository: UserRepository
    ): com.complexly.academy.domain.GetRecentlyViewedUseCase =
        com.complexly.academy.domain.GetRecentlyViewedUseCase(contentRepository, userRepository)

    companion object {
        private var creator: (() -> AppComponent)? = null

        fun init(creator: () -> AppComponent) {
            this.creator = creator
        }

        val instance: AppComponent by lazy {
            creator?.invoke() ?: error("AppComponent not initialized. Call AppComponent.init { ... } first.")
        }
    }
}
