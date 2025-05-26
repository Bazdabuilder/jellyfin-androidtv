package org.jellyfin.androidtv.ui.search

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.FragmentJellyseerrWebviewBinding
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.content.Context
import android.util.Log
// Note: AuthenticationRepositoryImpl is not directly used here for imports,
// but constants are duplicated for clarity as per instruction.

class JellyseerrWebViewFragment : Fragment() {

    private var _binding: FragmentJellyseerrWebviewBinding? = null
    private val binding get() = _binding!!

    private val jellyseerrUrl = "https://requests.jellylion.co.uk"
    // Constants for EncryptedSharedPreferences, must match those in AuthenticationRepositoryImpl
    companion object {
        fun newInstance() = JellyseerrWebViewFragment()
        private const val JELLYFIN_CREDENTIALS_PREFS = "jellyfin_credentials_prefs_secure"
        private const val KEY_USERNAME_PREFIX = "username_"
        private const val KEY_PASSWORD_PREFIX = "password_"
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJellyseerrWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var usernameRetrieved: String? = null
        var passwordRetrieved: String? = null

        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                requireContext(),
                JELLYFIN_CREDENTIALS_PREFS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Placeholder for obtaining serverId and userId.
            // These are needed to construct the actual keys for retrieving credentials.
            // For now, auto-login will be disabled as these IDs are not available in this fragment.
            Log.w("JellyseerrWebView", "ServerId and UserId are not available in this fragment. Auto-login to Jellyseerr is currently disabled. These IDs need to be passed to this fragment or retrieved from a shared ViewModel/session manager.")

            // Example of how it would work if serverId and userId were available:
            // val currentServerId: UUID? = ... // Obtain current server ID
            // val currentUserId: UUID? = ...   // Obtain current user ID
            // if (currentServerId != null && currentUserId != null) {
            //     val usernameKey = "$KEY_USERNAME_PREFIX${currentServerId}_$currentUserId"
            //     val passwordKey = "$KEY_PASSWORD_PREFIX${currentServerId}_$currentUserId"
            //     usernameRetrieved = sharedPreferences.getString(usernameKey, null)
            //     passwordRetrieved = sharedPreferences.getString(passwordKey, null)
            //     if (usernameRetrieved != null && passwordRetrieved != null) {
            //         Log.d("JellyseerrWebView", "Successfully retrieved stored credentials.")
            //     } else {
            //         Log.d("JellyseerrWebView", "No stored credentials found for the current user/server.")
            //     }
            // }

        } catch (e: Exception) {
            Log.e("JellyseerrWebView", "Error initializing or using EncryptedSharedPreferences: ", e)
        }

        binding.jellyseerrWebview.apply {
            settings.javaScriptEnabled = true
            // For debugging WebView content if needed
            // WebView.setWebContentsDebuggingEnabled(true)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Show loading indicator
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Hide loading indicator

                    if (url != null && url.contains("login", ignoreCase = true) && usernameRetrieved != null && passwordRetrieved != null) {
                        val jsScript = """
                            (function() {
                                let usernameField = document.getElementById('username') || document.getElementById('email') || document.querySelector('input[type="text"]') || document.querySelector('input[name*="user"]');
                                let passwordField = document.getElementById('password') || document.querySelector('input[type="password"]') || document.querySelector('input[name*="pass"]');
                                let loginButton = document.querySelector('button[type="submit"]') || document.querySelector('input[type="submit"]');

                                if (usernameField && passwordField) {
                                    usernameField.value = '${usernameRetrieved!!.replace("'", "\\'")}';
                                    passwordField.value = '${passwordRetrieved!!.replace("'", "\\'")}';
                                    
                                    if (loginButton) {
                                        loginButton.click();
                                    } else if (usernameField.form) {
                                        usernameField.form.submit();
                                    }
                                    return "Login attempt submitted.";
                                }
                                return "Username or password field not found.";
                            })();
                        """.trimIndent()

                        view?.evaluateJavascript(jsScript) { result ->
                            Log.d("JellyseerrWebView", "JavaScript execution result: $result")
                        }
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e("JellyseerrWebView", "WebView Error: $description (URL: $failingUrl)")
                }
            }
            loadUrl(jellyseerrUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.jellyseerrWebview.destroy()
        _binding = null
    }
}
