package com.fluxzen.ui_design.security

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityUtil @Inject constructor() {
    fun verifySignedMessage(context: Context, signedMessage: String): String? = signedMessage
    fun generateSignedMessage(context: Context, message: String): String = message
}
