package com.smallcloud.codify.account

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import com.smallcloud.codify.io.Connection
import com.smallcloud.codify.io.ConnectionStatus
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


class LoginStateService {
    private var inferenceTask: Future<*>? = null

    private var lastWebsiteLoginStatus: String = "OK"
    private var lastInferenceLoginStatus: String = "OK"

    init {
        inferenceTask = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            this::tryToInferenceLogin, 1, 1, TimeUnit.HOURS
        )
    }

    fun getLastWebsiteLoginStatus(): String {
        return lastWebsiteLoginStatus
    }

    fun getLastInferenceLoginStatus(): String {
        return lastInferenceLoginStatus
    }

    private fun tryToWebsiteLogin() {
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                Logger.getInstance("check_login").warn("call")
                lastWebsiteLoginStatus = checkLogin()
            } catch (e: Exception) {
                logError("check_login exception: $e")
            }
        }
    }

    private fun tryToInferenceLogin() {
        try {
            Logger.getInstance("inference_login").warn("call")
            lastInferenceLoginStatus = inferenceLogin()
        } catch (e: Exception) {
            logError("inference_login exception: $e")
        }
    }
}
