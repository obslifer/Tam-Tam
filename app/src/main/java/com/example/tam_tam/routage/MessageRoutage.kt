package com.example.tam_tam.routage

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import android.util.Log
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
class MessageRoutage(context: Context, myDeviceId:String) {

    private val TAG = "TamTam_Routage"
    private val SERVICE_ID = "com.example.tam_tam"

    private val gson = Gson()
    private val connectedEndpoints = mutableSetOf<String>() // on remplacera ces endpoints avec les enpoints uniques generer par le code des autres personnes
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            try {
                val message = Message.fromJson(String(payload.asBytes()!!))
                if (message!!.recipientId == myDeviceId) {
                    Log.d(TAG, "Message reçu: ${message.content}")
                } else if (message.intensity > 1) {
                    sendMessageToNearbyPeers(message.copy(intensity = message.intensity - 1))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la réception du payload", e)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            //Elle est juste la , pour la gestion de la progression du transfert
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            try {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'acceptation de la connexion", e)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpoints.add(endpointId)
                    Log.d(TAG, "Connecté à : $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connexion refusée par : $endpointId")
                }
                else -> {
                    Log.e(TAG, "Erreur de connexion avec : $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            Log.d(TAG, "Déconnecté de : $endpointId")
        }
    }

    // on verra bien si elle devient necessaire ou finalement pas dans le futur
//    fun startDiscovery() {
//        connectionsClient.startDiscovery(
//            SERVICE_ID,
//            endpointDiscoveryCallback,
//            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
//        )
//            .addOnSuccessListener {
//                Log.d(TAG, "Découverte démarrée")
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Erreur lors du démarrage de la découverte", e)
//            }
//    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection("MonNom", endpointId, connectionLifecycleCallback)
                .addOnSuccessListener {
                    Log.d(TAG, "Demande de connexion envoyée à : $endpointId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erreur lors de la demande de connexion", e)
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Pair perdu : $endpointId")
            connectedEndpoints.remove(endpointId)
        }
    }

//    fun sendMessage(message: String, recipientId: String) {
//        val messageObject = Message(message, recipientId, intensity = 10)
//        sendMessageToNearbyPeers(messageObject)
//    }

    private fun sendMessageToNearbyPeers(message: Message) {
        if (message.intensity > 0) {
            try {
                val payload = Payload.fromBytes(message.toJson().toByteArray())
                for (endpointId in connectedEndpoints) {
                    connectionsClient.sendPayload(endpointId, payload)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'envoi du message", e)
            }
        }
    }

    private fun Message.toJson(): String {
        return gson.toJson(this)
    }

}