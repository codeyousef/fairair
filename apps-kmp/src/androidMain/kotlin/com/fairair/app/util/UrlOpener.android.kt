package com.fairair.app.util

/**
 * Android implementation of URL opener using Android intents.
 */
actual object UrlOpener {
    
    // Context must be set by the Android Application class
    private var contextProvider: (() -> android.content.Context)? = null
    
    /**
     * Initialize with an Android context provider.
     * Should be called from Application.onCreate() or Activity.
     */
    fun init(provider: () -> android.content.Context) {
        contextProvider = provider
    }
    
    /**
     * Opens a URL in the default browser.
     */
    actual fun openUrl(url: String): Boolean {
        return try {
            val context = contextProvider?.invoke() ?: return false
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(url)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Opens the phone dialer with the given number.
     */
    actual fun openPhone(phoneNumber: String): Boolean {
        return try {
            val context = contextProvider?.invoke() ?: return false
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$cleanNumber")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Opens the default email client with a compose window.
     */
    actual fun openEmail(email: String): Boolean {
        return try {
            val context = contextProvider?.invoke() ?: return false
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:$email")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
