package dev.bikram.filepipe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.bikram.filepipe.update.UpdateChecker
import dev.bikram.filepipe.update.UpdateCheckerImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {
    @Binds
    abstract fun bindUpdateChecker(impl: UpdateCheckerImpl): UpdateChecker
}
