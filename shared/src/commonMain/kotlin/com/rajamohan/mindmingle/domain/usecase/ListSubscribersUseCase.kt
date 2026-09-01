package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.SubscriberPage
import com.rajamohan.mindmingle.domain.model.SubscriberStats
import com.rajamohan.mindmingle.domain.model.SubscriberStatusFilter
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

/** Admin-only: one filtered page of current and past subscribers. */
class ListSubscribersUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(
        status: SubscriberStatusFilter = SubscriberStatusFilter.ALL,
        planId: String = "",
        country: String = "",
        query: String = "",
        pageSize: Int = 25,
        cursor: String = ""
    ): Result<SubscriberPage> = repository.listSubscribers(
        status = status,
        planId = planId,
        country = country,
        query = query,
        pageSize = pageSize,
        cursor = cursor
    )
}

/** Admin-only: subscriber counts for the screen header. */
class GetSubscriberStatsUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(): Result<SubscriberStats> = repository.getSubscriberStats()
}
