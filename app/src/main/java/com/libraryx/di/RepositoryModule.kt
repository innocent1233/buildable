package com.libraryx.di

import com.libraryx.data.repository.LocalStudyLabRepository
import com.libraryx.data.repository.StudyLabRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the Solo-mode [StudyLabRepository] singleton. The SaaS-mode
 * [com.libraryx.data.repository.FirebaseStudyLabRepository] is *not* bound here because it
 * needs per-session parameters (subAdminUid/adminUid) — it's created on demand via its
 * injected `Factory`, exactly mirroring how SaasStudyLabBridge.tsx mounts
 * `<StudyLabProvider source="firebase" subAdminUid={...} />` only once a sub-admin is known.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocalStudyLabRepository(impl: LocalStudyLabRepository): StudyLabRepository
}
