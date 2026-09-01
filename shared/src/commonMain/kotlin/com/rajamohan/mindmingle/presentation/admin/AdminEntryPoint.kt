package com.rajamohan.mindmingle.presentation.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rajamohan.mindmingle.presentation.admin.component.AdminDashboardScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminDeletionRequestsScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminPlanPricingScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminSubscriberListScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminSupportChatScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminSupportListScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminUserDetailScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminUserListScreen

private sealed class AdminScreenState {
    data object Dashboard : AdminScreenState()
    data object UserList : AdminScreenState()
    data object PlanPricing : AdminScreenState()
    data object Subscribers : AdminScreenState()
    data class UserDetail(val uid: String) : AdminScreenState()
    data object SupportList : AdminScreenState()
    data object DeletionRequests : AdminScreenState()
    data class SupportChat(val uid: String, val userName: String) : AdminScreenState()
}

/**
 * Admin section, entered post sign-in once [com.rajamohan.mindmingle.presentation.login.viewmodel.AuthViewModel]
 * confirms the signed-in uid has an admins/{uid} doc — there is no separate admin login,
 * admins authenticate through the same phone-OTP/Google flow as regular users.
 */
@Composable
fun AdminEntryPoint(onSignOut: () -> Unit) {
    var currentScreen by remember { mutableStateOf<AdminScreenState>(AdminScreenState.Dashboard) }

    when (val screen = currentScreen) {
        AdminScreenState.Dashboard -> {
            AdminDashboardScreen(
                onManageUsers = { currentScreen = AdminScreenState.UserList },
                onManagePricing = { currentScreen = AdminScreenState.PlanPricing },
                onManageSubscribers = { currentScreen = AdminScreenState.Subscribers },
                onManageSupport = { currentScreen = AdminScreenState.SupportList },
                onManageDeletionRequests = { currentScreen = AdminScreenState.DeletionRequests },
                onSignOut = onSignOut
            )
        }
        AdminScreenState.DeletionRequests -> {
            AdminDeletionRequestsScreen(onBack = { currentScreen = AdminScreenState.Dashboard })
        }
        AdminScreenState.SupportList -> {
            AdminSupportListScreen(
                onThreadClick = { uid, userName -> currentScreen = AdminScreenState.SupportChat(uid, userName) },
                onBack = { currentScreen = AdminScreenState.Dashboard }
            )
        }
        is AdminScreenState.SupportChat -> {
            AdminSupportChatScreen(
                uid = screen.uid,
                userName = screen.userName,
                onBack = { currentScreen = AdminScreenState.SupportList }
            )
        }
        AdminScreenState.Subscribers -> {
            AdminSubscriberListScreen(
                // A subscriber row opens the same detail screen as a user row: the plan
                // controls and order history an admin wants next already live there.
                onSubscriberClick = { uid -> currentScreen = AdminScreenState.UserDetail(uid) },
                onBack = { currentScreen = AdminScreenState.Dashboard }
            )
        }
        AdminScreenState.PlanPricing -> {
            AdminPlanPricingScreen(onBack = { currentScreen = AdminScreenState.Dashboard })
        }
        AdminScreenState.UserList -> {
            AdminUserListScreen(
                onUserClick = { uid -> currentScreen = AdminScreenState.UserDetail(uid) },
                onBack = { currentScreen = AdminScreenState.Dashboard }
            )
        }
        is AdminScreenState.UserDetail -> {
            AdminUserDetailScreen(
                uid = screen.uid,
                onBack = { currentScreen = AdminScreenState.UserList },
                onUserDeleted = { currentScreen = AdminScreenState.UserList }
            )
        }
    }
}
