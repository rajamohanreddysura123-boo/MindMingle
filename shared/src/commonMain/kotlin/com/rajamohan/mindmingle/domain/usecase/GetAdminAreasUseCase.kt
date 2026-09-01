package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AdminAreas
import com.rajamohan.mindmingle.domain.model.AdminAreasRepository

/**
 * Region and district names for one country, for the filter sheet and profile setup.
 *
 * Reads the bundled `admin_areas/{ISO}.json` — no network, no seeding, no Firestore document to
 * create before a country's districts appear.
 */
class GetAdminAreasUseCase {
    suspend operator fun invoke(countryCode: String): AdminAreas =
        AdminAreasRepository.forCountry(countryCode)
}
