package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.AnonymousSession
import kotlinx.coroutines.flow.Flow

interface AnonymousChatRepository {
    suspend fun findPartner(uid: String): AnonymousSession?

    fun observeIncomingMessages(sessionId: String, selfUid: String): Flow<Pair<String, String>>

    fun observePartnerPresence(sessionId: String, selfUid: String): Flow<Boolean>

    suspend fun sendMessage(sessionId: String, selfUid: String, text: String): Boolean

    suspend fun skipPartner(uid: String)

    suspend fun leave(uid: String)
}
