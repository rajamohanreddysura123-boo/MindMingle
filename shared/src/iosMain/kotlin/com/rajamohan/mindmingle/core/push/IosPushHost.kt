package com.rajamohan.mindmingle.core.push

/**
 * The Swift side of push on iOS.
 *
 * Same shape as IosAdHost/IosPaymentHost: Gradle never links FirebaseMessaging, so `iosApp`
 * registers these closures at launch (see iosApp/iosApp/PushBridge.swift) and the shared code
 * calls them. Until that happens the bridge is not installed and push simply stays off.
 */
object IosPushHost {

    var isBridgeInstalled: Boolean = false
        private set

    private var permissionRequester: (((Boolean) -> Unit) -> Unit)? = null
    private var tokenProvider: (((String?) -> Unit) -> Unit)? = null

    private var refreshListener: ((String) -> Unit)? = null

    /**
     * @param requestPermission shows the iOS notification prompt and reports the outcome.
     * @param fetchToken hands back the current FCM registration token, or null.
     */
    fun install(
        requestPermission: ((Boolean) -> Unit) -> Unit,
        fetchToken: ((String?) -> Unit) -> Unit
    ) {
        permissionRequester = requestPermission
        tokenProvider = fetchToken
        isBridgeInstalled = true
    }

    /** Called from Swift when FCM rotates the token. */
    fun onTokenRefreshed(token: String) {
        refreshListener?.invoke(token)
    }

    internal fun setRefreshListener(listener: (String) -> Unit) {
        refreshListener = listener
    }

    internal fun requestPermission(onResult: (Boolean) -> Unit) {
        val requester = permissionRequester
        if (requester == null) {
            onResult(false)
            return
        }
        requester(onResult)
    }

    internal fun fetchToken(onResult: (String?) -> Unit) {
        val provider = tokenProvider
        if (provider == null) {
            onResult(null)
            return
        }
        provider(onResult)
    }
}
