package it.simoc.gestioneturni.data.account

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException

/** Messaggio comprensibile per chi usa l'app. */
fun messaggioErrore(e: Throwable): String = when (e) {
    is AuthRestException -> when (e.errorCode) {
        AuthErrorCode.InvalidCredentials -> "Email o password non corretti."
        AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> "Esiste già un account con questa email: usa \"Accedi\"."
        AuthErrorCode.WeakPassword -> "Password troppo debole: usane una di almeno 6 caratteri."
        AuthErrorCode.ValidationFailed -> "Controlla che l'email sia scritta giusta."
        else -> e.errorDescription
    }
    // Errori delle funzioni SQL (es. "Codice non valido").
    is RestException -> e.error
    is HttpRequestException, is HttpRequestTimeoutException -> "Nessuna connessione a internet. Riprova."
    else -> e.message ?: "Errore sconosciuto."
}
