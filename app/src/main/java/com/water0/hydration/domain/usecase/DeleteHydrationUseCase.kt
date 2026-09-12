package com.water0.hydration.domain.usecase

import com.water0.hydration.data.repository.HydrationRepository

class DeleteHydrationUseCase(
    private val repository: HydrationRepository
) {
    suspend operator fun invoke(entryId: Long) {
        repository.deleteEntry(entryId)
    }
}
