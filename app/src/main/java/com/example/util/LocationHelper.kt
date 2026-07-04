package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    /**
     * Tenta obter a localização atual.
     * Retorna "latitude, longitude" ou null se não tiver permissão / falhar.
     */
    suspend fun getLocalizacaoAtual(context: Context): String? {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        return suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            val priority = if (fineGranted)
                Priority.PRIORITY_HIGH_ACCURACY
            else
                Priority.PRIORITY_BALANCED_POWER_ACCURACY

            try {
                client.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val lat = String.format(java.util.Locale.US, "%.6f", location.latitude)
                            val lon = String.format(java.util.Locale.US, "%.6f", location.longitude)
                            continuation.resume("$lat, $lon")
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation { cts.cancel() }
        }
    }
}
