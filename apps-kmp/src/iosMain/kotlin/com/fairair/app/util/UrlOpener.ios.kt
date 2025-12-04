package com.fairair.app.util

/**
 * iOS implementation of URL opener using UIKit APIs.
 */
actual object UrlOpener {
    
    /**
     * Opens a URL using iOS UIApplication.
     */
    actual fun openUrl(url: String): Boolean {
        return try {
            val nsUrl = platform.Foundation.NSURL.URLWithString(url) ?: return false
            platform.UIKit.UIApplication.sharedApplication.openURL(nsUrl)
            true
        } catch (e: Exception) {
            println("Failed to open URL: $url - ${e.message}")
            false
        }
    }
    
    /**
     * Opens the phone dialer with the given number.
     */
    actual fun openPhone(phoneNumber: String): Boolean {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val telUrl = platform.Foundation.NSURL.URLWithString("tel:$cleanNumber") ?: return false
            platform.UIKit.UIApplication.sharedApplication.openURL(telUrl)
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
            val mailtoUrl = platform.Foundation.NSURL.URLWithString("mailto:$email") ?: return false
            platform.UIKit.UIApplication.sharedApplication.openURL(mailtoUrl)
            true
        } catch (e: Exception) {
            println("Failed to open email: $email - ${e.message}")
            false
        }
    }
}
