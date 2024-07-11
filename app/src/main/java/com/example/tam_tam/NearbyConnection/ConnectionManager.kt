package com.example.tam_tam.NearbyConnection

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

// Classe pour gérer les connexions Nearby
class ConnectionManager(private val context: Context) {
    // Client de connexions Nearby
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    // ID du service
    private val serviceId = "com.example.tam_tam.SERVICE_ID"

    // Démarre la publicité et la découverte
    fun startAdvertisingAndDiscovery(endpointName: String) {
        startAdvertising(endpointName)
        startDiscovery()
    }

    // Démarre la publicité pour permettre aux autres appareils de découvrir cet appareil
    private fun startAdvertising(endpointName: String) {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            endpointName,
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d("ConnectionManager", "Started Advertising")
        }.addOnFailureListener {
            Log.e("ConnectionManager", "Failed to start advertising: ${it.message}")
        }
    }

    // Démarre la découverte pour trouver les autres appareils à proximité
    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d("ConnectionManager", "Started Discovery")
        }.addOnFailureListener {
            Log.e("ConnectionManager", "Failed to start discovery: ${it.message}")
        }
    }

    // Callback pour le cycle de vie des connexions
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Accepte la connexion
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    // Connexion réussie
                    Log.d("ConnectionManager", "Connected to $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // Connexion rejetée
                    Log.d("ConnectionManager", "Connection rejected by $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    // Erreur de connexion
                    Log.d("ConnectionManager", "Connection error with $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            // Déconnexion
            Log.d("ConnectionManager", "Disconnected from $endpointId")
        }
    }

    // Callback pour la découverte des endpoints
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        // Demande une connexion au endpoint découvert
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(endpointId, info.endpointName, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            // Endpoint perdu
            Log.d("ConnectionManager", "Lost endpoint $endpointId")
        }
    }

    // Callback pour les payloads (données échangées)
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // Gère la réception des payloads
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Gère les mises à jour de transfert des payloads
        }
    }
}