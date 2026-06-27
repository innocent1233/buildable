package com.libraryx.ui.navigation

import com.libraryx.data.model.SubAdminDoc
import com.libraryx.data.repository.FirebaseStudyLabRepository
import com.libraryx.data.repository.StudyLabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors src/components/SaasStudyLabBridge.tsx: once a sub-admin (lab owner) is known,
 * that bridge mounted `<StudyLabProvider source="firebase" subAdminUid={subAdmin.uid}
 * adminUid={subAdmin.adminUid}>` around all nested SaaS screens. Here, the same "mount once
 * the session is known" effect is achieved by lazily creating one
 * [FirebaseStudyLabRepository] per login session and sharing it with every screen
 * ViewModel through this singleton holder.
 */
@Singleton
class SaasSessionHolder @Inject constructor(
    private val repositoryFactory: FirebaseStudyLabRepository.Factory
) {
    private val _repository = MutableStateFlow<StudyLabRepository?>(null)
    val repository: StateFlow<StudyLabRepository?> = _repository.asStateFlow()

    /** Call once `SaasAuthViewModel.state.subAdmin` becomes non-null (see StudyLabNavGraph). */
    fun bind(subAdmin: SubAdminDoc) {
        val current = _repository.value
        if (current is FirebaseStudyLabRepository) return // already bound to this session
        _repository.value = repositoryFactory.create(subAdmin.uid, subAdmin.adminUid)
    }

    /** Call on logout — mirrors the bridge unmounting `<StudyLabProvider>` when `subAdmin` clears. */
    fun clear() {
        _repository.value = null
    }
}
