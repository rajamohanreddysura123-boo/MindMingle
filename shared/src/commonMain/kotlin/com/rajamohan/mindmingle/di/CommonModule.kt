package com.rajamohan.mindmingle.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import com.rajamohan.mindmingle.core.AppContext
import com.rajamohan.mindmingle.core.KSafe
import com.rajamohan.mindmingle.core.LMPreferences
import com.rajamohan.mindmingle.core.createKSafe
import com.rajamohan.mindmingle.data.local.source.EphemeralMessageLocalSource
import com.rajamohan.mindmingle.data.local.source.MindMingleDatabaseProvider
import com.rajamohan.mindmingle.data.remote.source.AnonymousChatRemoteSource
import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.data.respository.AnonymousChatRepositoryImpl
import com.rajamohan.mindmingle.data.respository.EphemeralMessageStoreImpl
import com.rajamohan.mindmingle.data.respository.MindMingleAdminRepositoryImpl
import com.rajamohan.mindmingle.data.respository.MindMingleLocalRepositoryImpl
import com.rajamohan.mindmingle.data.respository.MindMingleRemoteRepositoryImpl
import com.rajamohan.mindmingle.data.respository.SubscriptionRepositoryImpl
import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository
import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository
import com.rajamohan.mindmingle.domain.usecase.AdminCancelSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.AdminSetSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.DeactivateMyAccountUseCase
import com.rajamohan.mindmingle.domain.usecase.ListDeletionRequestsUseCase
import com.rajamohan.mindmingle.domain.usecase.RequestAccountDeletionUseCase
import com.rajamohan.mindmingle.domain.usecase.GetBillingHistoryUseCase
import com.rajamohan.mindmingle.domain.usecase.CreatePaymentLinkUseCase
import com.rajamohan.mindmingle.domain.usecase.CreatePaymentOrderUseCase
import com.rajamohan.mindmingle.domain.usecase.GetPlanCatalogUseCase
import com.rajamohan.mindmingle.domain.usecase.ResetPlanCatalogUseCase
import com.rajamohan.mindmingle.domain.usecase.SavePlanCatalogUseCase
import com.rajamohan.mindmingle.data.respository.PushRepositoryImpl
import com.rajamohan.mindmingle.domain.repository.PushRepository
import com.rajamohan.mindmingle.domain.usecase.GetNotificationPrefsUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveIncomingLikeAlertsUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveMessageAlertsUseCase
import com.rajamohan.mindmingle.domain.usecase.RegisterPushDeviceUseCase
import com.rajamohan.mindmingle.domain.usecase.SaveNotificationPrefsUseCase
import com.rajamohan.mindmingle.domain.usecase.UnregisterPushDeviceUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.RefreshMyLocationUseCase
import com.rajamohan.mindmingle.domain.usecase.GetSubscriberStatsUseCase
import com.rajamohan.mindmingle.domain.usecase.ListSubscribersUseCase
import com.rajamohan.mindmingle.domain.usecase.RecordPaymentFailureUseCase
import com.rajamohan.mindmingle.domain.usecase.VerifyPaymentUseCase
import com.rajamohan.mindmingle.domain.usecase.GetPendingAnonymousMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.LeaveAnonymousChatUseCase
import com.rajamohan.mindmingle.domain.usecase.MarkAnonymousMessagesSeenUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveAnonymousMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveAnonymousPartnerPresenceUseCase
import com.rajamohan.mindmingle.domain.usecase.PurgeSeenAnonymousMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.SendAnonymousMessageUseCase
import com.rajamohan.mindmingle.domain.usecase.SkipAnonymousPartnerUseCase
import com.rajamohan.mindmingle.domain.usecase.StartAnonymousChatUseCase
import com.rajamohan.mindmingle.domain.usecase.DeleteUserCascadeUseCase
import com.rajamohan.mindmingle.domain.usecase.GetAdConfigUseCase
import com.rajamohan.mindmingle.domain.usecase.GetAppUpdateConfigUseCase
import com.rajamohan.mindmingle.domain.usecase.GetSentLikeUidsUseCase
import com.rajamohan.mindmingle.domain.usecase.IgnoreIncomingLikeUseCase
import com.rajamohan.mindmingle.domain.usecase.GetAdminStatsUseCase
import com.rajamohan.mindmingle.domain.usecase.DeleteSeenMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetConversationsUseCase
import com.rajamohan.mindmingle.domain.usecase.GetDiscoverProfilesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetIncomingLikesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetAdminAreasUseCase
import com.rajamohan.mindmingle.domain.usecase.GetProfileStatsUseCase
import com.rajamohan.mindmingle.domain.usecase.GetUserProfileUseCase
import com.rajamohan.mindmingle.domain.usecase.LikeUserUseCase
import com.rajamohan.mindmingle.domain.usecase.ListAllUsersUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveIncomingLikesUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveSupportMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveSupportThreadsUseCase
import com.rajamohan.mindmingle.domain.usecase.SendMessageUseCase
import com.rajamohan.mindmingle.domain.usecase.SendSupportMessageUseCase
import com.rajamohan.mindmingle.domain.usecase.SetPremiumFlagUseCase
import com.rajamohan.mindmingle.domain.usecase.SetUserDisabledUseCase
import com.rajamohan.mindmingle.domain.usecase.UnbanUserUseCase
import com.rajamohan.mindmingle.presentation.account.viewmodel.AccountSettingsViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminDashboardViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminDeletionRequestsViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminPlanPricingViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminUserDetailViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminSupportListViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminSubscriberListViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminUserListViewModel
import com.rajamohan.mindmingle.presentation.anonymous.viewmodel.AnonymousChatViewModel
import com.rajamohan.mindmingle.presentation.chat.viewmodel.ChatViewModel
import com.rajamohan.mindmingle.presentation.home.viewmodel.HomeViewModel
import com.rajamohan.mindmingle.presentation.notifications.AlertsViewModel
import com.rajamohan.mindmingle.presentation.likes.viewmodel.LikesViewModel
import com.rajamohan.mindmingle.presentation.login.viewmodel.AuthViewModel
import com.rajamohan.mindmingle.presentation.premium.viewmodel.PremiumViewModel
import com.rajamohan.mindmingle.presentation.profile.viewmodel.ProfileViewModel
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupViewModel
import com.rajamohan.mindmingle.presentation.support.viewmodel.SupportChatViewModel
import kotlin.concurrent.Volatile

val coreModule = module {
    single {
        AppContext
    }
    single<KSafe> {
        createKSafe()
    }
    single {
        LMPreferences()
    }
}

val dataModule = module {
    single {
        MindMingleFirebaseProvider()
    }

    single {
        MindMingleDatabaseProvider(
            preferences = get()
        )
    }

    single {
        AnonymousChatRemoteSource()
    }

    single {
        EphemeralMessageLocalSource(
            preferences = get()
        )
    }
}

val domainModule = module {
    // Push was scaffolded but never bound: nothing in the graph could build a PushRepository, so
    // no device token was ever registered and no notification preference was ever read.
    single<PushRepository> {
        PushRepositoryImpl(
            mindMingleFirebaseProvider = get()
        )
    }

    single<MindMingleRemoteRepository> {
        MindMingleRemoteRepositoryImpl(
            mindMingleFirebaseProvider = get()
        )
    }

    single<MindMingleLocalRepository> {
        MindMingleLocalRepositoryImpl(
            mindMingleDatabaseProvider = get()
        )
    }

    single<MindMingleAdminRepository> {
        MindMingleAdminRepositoryImpl(
            mindMingleFirebaseProvider = get()
        )
    }

    single<AnonymousChatRepository> {
        AnonymousChatRepositoryImpl(
            remoteSource = get()
        )
    }

    single<SubscriptionRepository> {
        SubscriptionRepositoryImpl(
            mindMingleFirebaseProvider = get()
        )
    }

    single<EphemeralMessageStore> {
        EphemeralMessageStoreImpl(
            localSource = get()
        )
    }

    factory {
        GetDiscoverProfilesUseCase(
            repository = get()
        )
    }

    factory {
        GetAdConfigUseCase(
            repository = get()
        )
    }

    factory {
        GetAppUpdateConfigUseCase(
            repository = get()
        )
    }

    factory {
        GetSentLikeUidsUseCase(
            repository = get()
        )
    }

    factory {
        IgnoreIncomingLikeUseCase(
            repository = get()
        )
    }

    factory {
        LikeUserUseCase(
            repository = get()
        )
    }

    factory {
        CreatePaymentLinkUseCase(
            repository = get()
        )
    }

    factory {
        CreatePaymentOrderUseCase(
            repository = get()
        )
    }

    factory {
        GetPlanCatalogUseCase(
            repository = get()
        )
    }

    factory {
        GetBillingHistoryUseCase(
            repository = get()
        )
    }

    factory {
        AdminSetSubscriptionUseCase(
            repository = get()
        )
    }

    factory {
        AdminCancelSubscriptionUseCase(
            repository = get()
        )
    }

    factory {
        RequestAccountDeletionUseCase(
            repository = get()
        )
    }

    factory {
        DeactivateMyAccountUseCase(
            repository = get()
        )
    }

    factory {
        ListDeletionRequestsUseCase(
            repository = get()
        )
    }

    factory {
        SavePlanCatalogUseCase(
            repository = get()
        )
    }

    factory {
        ResetPlanCatalogUseCase(
            repository = get()
        )
    }

    factory {
        VerifyPaymentUseCase(
            repository = get()
        )
    }

    factory {
        RecordPaymentFailureUseCase(
            repository = get()
        )
    }

    factory {
        ObserveSubscriptionUseCase(
            repository = get()
        )
    }

    factory {
        GetIncomingLikesUseCase(
            repository = get()
        )
    }

    factory {
        GetUserProfileUseCase(
            repository = get()
        )
    }

    factory {
        SetPremiumFlagUseCase(
            repository = get()
        )
    }

    factory {
        GetProfileStatsUseCase(
            repository = get()
        )
    }

    factory {
        RefreshMyLocationUseCase(
            repository = get(),
            localRepository = get()
        )
    }

    factory {
        GetAdminAreasUseCase()
    }

    factory {
        ObserveIncomingLikeAlertsUseCase(
            repository = get()
        )
    }

    factory {
        ObserveMessageAlertsUseCase(
            repository = get()
        )
    }

    factory {
        RegisterPushDeviceUseCase(
            repository = get()
        )
    }

    factory {
        UnregisterPushDeviceUseCase(
            repository = get()
        )
    }

    factory {
        GetNotificationPrefsUseCase(
            repository = get()
        )
    }

    factory {
        SaveNotificationPrefsUseCase(
            repository = get()
        )
    }

    factory {
        ObserveIncomingLikesUseCase(
            repository = get()
        )
    }

    factory {
        GetConversationsUseCase(
            repository = get()
        )
    }

    factory {
        ObserveMessagesUseCase(
            repository = get()
        )
    }

    factory {
        SendMessageUseCase(
            repository = get()
        )
    }

    factory {
        DeleteSeenMessagesUseCase(
            repository = get()
        )
    }

    factory {
        ObserveSupportMessagesUseCase(
            repository = get()
        )
    }

    factory {
        SendSupportMessageUseCase(
            repository = get()
        )
    }

    factory {
        ObserveSupportThreadsUseCase(
            repository = get()
        )
    }

    factory {
        ListAllUsersUseCase(
            repository = get()
        )
    }

    factory {
        ListSubscribersUseCase(
            repository = get()
        )
    }

    factory {
        GetSubscriberStatsUseCase(
            repository = get()
        )
    }

    factory {
        GetAdminStatsUseCase(
            repository = get()
        )
    }

    factory {
        SetUserDisabledUseCase(
            repository = get()
        )
    }

    factory {
        DeleteUserCascadeUseCase(
            repository = get()
        )
    }

    factory {
        UnbanUserUseCase(
            repository = get()
        )
    }

    factory {
        StartAnonymousChatUseCase(
            repository = get()
        )
    }

    factory {
        ObserveAnonymousMessagesUseCase(
            repository = get(),
            store = get()
        )
    }

    factory {
        ObserveAnonymousPartnerPresenceUseCase(
            repository = get()
        )
    }

    factory {
        SendAnonymousMessageUseCase(
            repository = get()
        )
    }

    factory {
        SkipAnonymousPartnerUseCase(
            repository = get(),
            store = get()
        )
    }

    factory {
        LeaveAnonymousChatUseCase(
            repository = get(),
            store = get()
        )
    }

    factory {
        MarkAnonymousMessagesSeenUseCase(
            store = get()
        )
    }

    factory {
        GetPendingAnonymousMessagesUseCase(
            store = get()
        )
    }

    factory {
        PurgeSeenAnonymousMessagesUseCase(
            store = get()
        )
    }
}

val presentationModule = module {
    factory {
        AuthViewModel(
            mindMingleLocalRepository = get(),
            mindMingleRemoteRepository = get(),
            unregisterPushDeviceUseCase = get()
        )
    }

    factory {
        ProfileSetupViewModel(
            mindMingleLocalRepository = get(),
            mindMingleRemoteRepository = get()
        )
    }

    factory {
        AlertsViewModel(
            observeIncomingLikeAlertsUseCase = get(),
            observeMessageAlertsUseCase = get(),
            getNotificationPrefsUseCase = get(),
            registerPushDeviceUseCase = get(),
            mindMingleLocalRepository = get()
        )
    }

    factory {
        HomeViewModel(
            getDiscoverProfilesUseCase = get(),
            likeUserUseCase = get(),
            getUserProfileUseCase = get(),
            getAdConfigUseCase = get(),
            observeSubscriptionUseCase = get(),
            setPremiumFlagUseCase = get(),
            refreshMyLocationUseCase = get(),
            getAdminAreasUseCase = get(),
            mindMingleLocalRepository = get()
        )
    }

    factory {
        PremiumViewModel(
            createPaymentOrderUseCase = get(),
            createPaymentLinkUseCase = get(),
            verifyPaymentUseCase = get(),
            recordPaymentFailureUseCase = get(),
            observeSubscriptionUseCase = get(),
            getPlanCatalogUseCase = get(),
            getUserProfileUseCase = get()
        )
    }

    factory {
        AdminPlanPricingViewModel(
            getPlanCatalogUseCase = get(),
            savePlanCatalogUseCase = get(),
            resetPlanCatalogUseCase = get()
        )
    }

    factory {
        LikesViewModel(
            observeIncomingLikesUseCase = get(),
            getSentLikeUidsUseCase = get(),
            likeUserUseCase = get(),
            ignoreIncomingLikeUseCase = get()
        )
    }

    factory {
        ChatViewModel(
            getConversationsUseCase = get(),
            observeMessagesUseCase = get(),
            sendMessageUseCase = get(),
            deleteSeenMessagesUseCase = get()
        )
    }

    factory {
        SupportChatViewModel(
            observeSupportMessagesUseCase = get(),
            sendSupportMessageUseCase = get()
        )
    }

    factory {
        AdminSupportListViewModel(
            observeSupportThreadsUseCase = get()
        )
    }

    factory {
        AnonymousChatViewModel(
            startAnonymousChatUseCase = get(),
            observeAnonymousMessagesUseCase = get(),
            observeAnonymousPartnerPresenceUseCase = get(),
            sendAnonymousMessageUseCase = get(),
            skipAnonymousPartnerUseCase = get(),
            leaveAnonymousChatUseCase = get(),
            markAnonymousMessagesSeenUseCase = get(),
            getPendingAnonymousMessagesUseCase = get(),
            purgeSeenAnonymousMessagesUseCase = get()
        )
    }

    factory {
        ProfileViewModel(
            getUserProfileUseCase = get(),
            getProfileStatsUseCase = get(),
            observeSubscriptionUseCase = get(),
            getBillingHistoryUseCase = get(),
            mindMingleRemoteRepository = get()
        )
    }

    factory {
        AccountSettingsViewModel(
            deactivateMyAccountUseCase = get(),
            requestAccountDeletionUseCase = get(),
            getNotificationPrefsUseCase = get(),
            saveNotificationPrefsUseCase = get(),
            mindMingleRemoteRepository = get()
        )
    }

    factory {
        AdminDashboardViewModel(
            getAdminStatsUseCase = get()
        )
    }

    factory {
        AdminDeletionRequestsViewModel(
            listDeletionRequestsUseCase = get(),
            deleteUserCascadeUseCase = get(),
            mindMingleRemoteRepository = get()
        )
    }

    factory {
        AdminUserListViewModel(
            listAllUsersUseCase = get()
        )
    }

    factory {
        AdminSubscriberListViewModel(
            listSubscribersUseCase = get(),
            getSubscriberStatsUseCase = get()
        )
    }

    factory {
        AdminUserDetailViewModel(
            getUserProfileUseCase = get(),
            setUserDisabledUseCase = get(),
            unbanUserUseCase = get(),
            deleteUserCascadeUseCase = get(),
            getBillingHistoryUseCase = get(),
            adminSetSubscriptionUseCase = get(),
            adminCancelSubscriptionUseCase = get(),
            mindMingleRemoteRepository = get()
        )
    }
}

fun getCommonModules() = presentationModule + dataModule + domainModule + coreModule

object KoinInitializer {

    @Volatile
    private var koinApp: KoinApplication? = null

    fun initKoin(
        commonModules: List<Module> = getCommonModules(),
        appDeclaration: KoinAppDeclaration = {}
    ) {
        if (koinApp == null) {
            Napier.base(DebugAntilog())
            Napier.i {
                "No koin instance found creating one"
            }
            koinApp = startKoin {
                modules(commonModules + platformModule)
                appDeclaration()
            }
        }
    }
}