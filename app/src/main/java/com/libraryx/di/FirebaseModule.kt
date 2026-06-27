package com.libraryx.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mirrors src/lib/firebase.ts (`getFirebase()` / `isFirebaseConfigured()`).
 *
 * On Android, Firebase configuration comes from `google-services.json` (applied by the
 * google-services Gradle plugin) rather than the inline `firebaseConfig` object the web
 * app hard-codes — see the migration report for the real config values to carry over.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
