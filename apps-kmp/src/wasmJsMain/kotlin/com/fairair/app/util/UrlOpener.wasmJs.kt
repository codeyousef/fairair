package com.fairair.app.util

import kotlinx.browser.window

/**
 * WASM/JS implementation of URL opener using browser APIs.
 */
actual object UrlOpener {
    
    /**
     * Opens a URL in a new browser tab.
     */
    actual fun openUrl(url: String): Boolean {
        return try {
            window.open(url, "_blank")
            true
        } catch (e: Exception) {
            println("Failed to open URL: $url - ${e.message}")
            false
        }
    }
    
    /**
     * Opens the phone dialer (on mobile) or copies number (on desktop).
     * Note: tel: links work on mobile but may not on desktop browsers.
     */
    actual fun openPhone(phoneNumber: String): Boolean {
        return try {
            val cleanNumber = if (phoneNumber.startsWith("tel:")) phoneNumber else "tel:$phoneNumber"
            window.open(cleanNumber, "_self")
            true
        } catch (e: Exception) {
            println("Failed to open phone: $phoneNumber - ${e.message}")
            false
        }
    }
    
    /**
     * Opens the default email client with a compose window.
     */
    actual fun openEmail(email: String): Boolean {
        return try {
            val mailtoUrl = if (email.startsWith("mailto:")) email else "mailto:$email"
            window.open(mailtoUrl, "_self")
            true
        } catch (e: Exception) {
            println("Failed to open email: $email - ${e.message}")
            false
        }
    }
}
