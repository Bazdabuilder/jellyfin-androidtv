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
        private const val ARG_SERVER_ID = "server_id"
        private const val ARG_USER_ID = "user_id"

        fun newInstance(serverId: java.util.UUID, userId: java.util.UUID): JellyseerrWebViewFragment {
            val fragment = JellyseerrWebViewFragment()
            val args = Bundle().apply {
                putSerializable(ARG_SERVER_ID, serverId)
                putSerializable(ARG_USER_ID, userId)
            }
            fragment.arguments = args
            return fragment
        }

        // Keep existing SharedPreferences constants here as well
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

        val serverId = arguments?.getSerializable(ARG_SERVER_ID) as? java.util.UUID
        val userId = arguments?.getSerializable(ARG_USER_ID) as? java.util.UUID

        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                requireContext(),
                JELLYFIN_CREDENTIALS_PREFS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            if (serverId != null && userId != null) {
                val usernameKey = "$KEY_USERNAME_PREFIX${serverId}_$userId"
                val passwordKey = "$KEY_PASSWORD_PREFIX${serverId}_$userId"
                usernameRetrieved = sharedPreferences.getString(usernameKey, null)
                passwordRetrieved = sharedPreferences.getString(passwordKey, null)

                if (usernameRetrieved != null && passwordRetrieved != null) {
                    Log.d("JellyseerrWebView", "Successfully retrieved stored credentials for user $userId on server $serverId.")
                } else {
                    Log.d("JellyseerrWebView", "No stored credentials found for user $userId on server $serverId.")
                }
            } else {
                Log.w("JellyseerrWebView", "ServerId or UserId not provided in arguments. Cannot retrieve credentials for auto-login.")
            }

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
                                let usernameField = document.querySelector('input[name="username"]');
                                let passwordField = document.getElementById('password');
                                let loginButton = document.querySelector('button[type="submit"]');

                                if (usernameField && passwordField) {
                                    usernameField.value = '${usernameRetrieved!!.replace("'", "\\'")}';
                                    passwordField.value = '${passwordRetrieved!!.replace("'", "\\'")}';
                                    
                                    if (loginButton) {
                                        loginButton.click();
                                    } else if (usernameField.form) { // Fallback if button not easily found
                                        usernameField.form.submit();
                                    }
                                    return "Login attempt submitted with updated selectors.";
                                }
                                return "Username or password field not found with updated selectors.";
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
