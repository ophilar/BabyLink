package com.fluxzen.babybeam

import com.fluxzen.ui_design.sync.NearbyTransportLayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MockDependencyInjection {
    @Provides
    @Singleton
    fun provideNearbyTransportLayer(): NearbyTransportLayer {
        return NearbyTransportLayer()
    }
}
