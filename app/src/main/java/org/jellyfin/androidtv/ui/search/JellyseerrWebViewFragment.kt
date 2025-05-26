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
// Import for EncryptedSharedPreferences (actual import path might vary based on library used)
// import androidx.security.crypto.EncryptedSharedPreferences
// import androidx.security.crypto.MasterKeys
import android.content.Context
import android.util.Log

class JellyseerrWebViewFragment : Fragment() {

    private var _binding: FragmentJellyseerrWebviewBinding? = null
    private val binding get() = _binding!!

    private val jellyseerrUrl = "https://requests.jellylion.co.uk"
    private val JELLYFIN_CREDENTIALS_PREFS = "jellyfin_credentials_prefs" // For EncryptedSharedPreferences
    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"


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

        // Retrieve stored credentials (using EncryptedSharedPreferences example)
        // Note: Actual implementation of credential retrieval will depend on Step 1's final outcome.
        // This is a placeholder to illustrate where it fits.
        val sharedPreferences = try {
            requireContext().getSharedPreferences(JELLYFIN_CREDENTIALS_PREFS, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e("JellyseerrWebView", "Error getting SharedPreferences: ", e)
            null
        }

        val username = sharedPreferences?.getString(KEY_USERNAME, null)
        val password = sharedPreferences?.getString(KEY_PASSWORD, null)

        binding.jellyseerrWebview.apply {
            settings.javaScriptEnabled = true
            // For debugging WebView content if needed (requires enabling in WebView settings)
            // WebView.setWebContentsDebuggingEnabled(true) 

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Show loading indicator if desired
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Hide loading indicator

                    if (url != null && url.contains("login", ignoreCase = true) && username != null && password != null) {
                        // The JavaScript selectors (getElementById, querySelector) and form submission logic
                        // are generic examples and MUST be verified against the actual Jellyseerr login page HTML structure.
                        val jsScript = """
                            (function() {
                                let usernameField = document.getElementById('username') || document.getElementById('email') || document.querySelector('input[type="text"]') || document.querySelector('input[name*="user"]');
                                let passwordField = document.getElementById('password') || document.querySelector('input[type="password"]') || document.querySelector('input[name*="pass"]');
                                let loginButton = document.querySelector('button[type="submit"]') || document.querySelector('input[type="submit"]');

                                if (usernameField && passwordField) {
                                    usernameField.value = '${username.replace("'", "\\'")}';
                                    passwordField.value = '${password.replace("'", "\\'")}';
                                    
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

    companion object {
        fun newInstance() = JellyseerrWebViewFragment()
    }
}
