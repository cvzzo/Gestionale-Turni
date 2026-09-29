package it.simoc.gestioneturni.data.remoto

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import it.simoc.gestioneturni.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabase(): SupabaseClient = createSupabaseClient(
        // Valori segnaposto se local.properties non è configurato: l'app lo segnala all'avvio.
        supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://non-configurato.supabase.co" },
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "non-configurato" },
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
