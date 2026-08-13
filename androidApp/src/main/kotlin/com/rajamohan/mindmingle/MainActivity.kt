package com.rajamohan.mindmingle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rajamohan.mindmingle.core.AppContext
import com.rajamohan.mindmingle.core.media.ImagePicker
import com.rajamohan.mindmingle.core.payments.RazorpayBridge
import com.rajamohan.mindmingle.data.remote.source.GoogleAuthLauncher
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

/**
 * Razorpay hands its checkout result to the activity that opened it, so MainActivity
 * implements the listener and forwards it to RazorpayBridge, where the shared
 * PaymentPlatform coroutine is waiting for it.
 */
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppContext.set(this)

        setContent {
            App()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        GoogleAuthLauncher.handleActivityResult(requestCode, resultCode, data)
        ImagePicker.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        RazorpayBridge.handlePaymentSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        RazorpayBridge.handlePaymentError(errorCode, response, paymentData)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}