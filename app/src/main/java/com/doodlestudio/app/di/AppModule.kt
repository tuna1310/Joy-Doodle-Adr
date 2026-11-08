package com.doodlestudio.app.di

import androidx.room.Room
import com.doodlestudio.app.data.local.AppDatabase
import com.doodlestudio.app.data.preferences.SettingsDataStore
import com.doodlestudio.app.data.repository.ArtworkRepositoryImpl
import com.doodlestudio.app.data.repository.TemplateRepositoryImpl
import com.doodlestudio.app.domain.repository.ArtworkRepository
import com.doodlestudio.app.domain.repository.TemplateRepository
import com.doodlestudio.app.ui.screens.editor.EditorViewModel
import com.doodlestudio.app.ui.screens.gallery.GalleryViewModel
import com.doodlestudio.app.ui.screens.home.HomeViewModel
import com.doodlestudio.app.ui.screens.library.LibraryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "doodle_studio.db"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDatabase>().artworkDao() }
}

val repositoryModule = module {
    single { SettingsDataStore(androidContext()) }
    single<TemplateRepository> { TemplateRepositoryImpl(androidContext()) }
    single<ArtworkRepository> { ArtworkRepositoryImpl(get(), androidContext()) }
    single { com.doodlestudio.app.util.SoundEffectManager(androidContext(), get()) }
}

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::GalleryViewModel)
    viewModelOf(::EditorViewModel)
    viewModelOf(::LibraryViewModel)
}

val appModules = listOf(databaseModule, repositoryModule, viewModelModule)
