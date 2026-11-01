package com.ezansaati.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    /** Güncel konumu döner; alınamazsa son bilinen konumu, o da yoksa null. İzin çağırandan önce alınmış olmalı. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cancellation = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location)
                } else {
                    client.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cancellation.cancel() }
    }
}
