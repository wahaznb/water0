package com.water0.hydration.domain.usecase

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.repository.HydrationRepository

class LogHydrationUseCase(
    private val repository: HydrationRepository
) {

    suspend operator fun invoke(
        amountMl: Int,
        type: HydrationEntry.DrinkType = HydrationEntry.DrinkType.WATER,
        source: HydrationEntry.EntrySource = HydrationEntry.EntrySource.MANUAL
    ) {
        val entry = HydrationEntry(
            amountMl = amountMl,
            timestamp = System.currentTimeMillis(),
            type = type,
            source = source
        )
        repository.insertEntry(entry)
        repository.onWaterLogged(amountMl, type)
    }

    suspend fun invoke(entry: HydrationEntry) {
        repository.insertEntry(entry)
        repository.onWaterLogged(entry.amountMl, entry.type)
    }
}