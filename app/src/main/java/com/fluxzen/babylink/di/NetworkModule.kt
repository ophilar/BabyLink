package com.fluxzen.babylink.di

import android.content.Context
import com.fluxzen.ui_design.sync.NearbyTransportLayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNearbyTransportLayer(
        @ApplicationContext context: Context
    ): NearbyTransportLayer {
        return NearbyTransportLayer(context)
    }
}
