package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner

object PermissionManager {

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null


    // Initialize for both FragmentActivity and Fragment
    fun init(owner: LifecycleOwner) {
        when (owner) {
            is FragmentActivity -> {
                permissionLauncher = owner.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    handlePermissionResult(isGranted)
                }
            }
            is Fragment -> {
                permissionLauncher = owner.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    handlePermissionResult(isGranted)
                }
            }
            else -> throw IllegalArgumentException("Unsupported LifecycleOwner type: ${owner::class.java.name}")
        }
    }

    // Request permission
    fun requestPermission(
        context: Context,
        permission: String,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires specific media permissions
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onPermissionGranted?.invoke()
            } else {
                permissionLauncher?.launch(permission)
            }
        } else {
            // Fallback for Android 12 and below
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onPermissionGranted?.invoke()
            } else {
                permissionLauncher?.launch(permission)
            }
        }
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            onPermissionGranted?.invoke()
        } else {
            onPermissionDenied?.invoke()
        }
    }
}