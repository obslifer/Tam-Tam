package com.example.tam_tam

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.tam_tam.models.Message
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object NearbyService {
    private const val TAG = "NearbyService"
    private const val SERVICE_ID = "com.example.messenger.SERVICE_ID"
    private lateinit var context: Context
    private lateinit var currentUserPhoneNumber: String

    data class DiscoveredEndpoint(val id: String, val name: String, var available: Boolean)

    fun start(context: Context, phoneNumber: String) {
        this.context = context
        this.currentUserPhoneNumber = phoneNumber
        DatabaseHelper.init(context)
        startDiscovery()
        startAdvertising()
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                Log.d(TAG, "Discovery started")
            }
            .addOnFailureListener {
                Log.e(TAG, "Discovery failed", it)
            }
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                currentUserPhoneNumber,
                SERVICE_ID,
                connectionLifecycleCallback,
                options
            )
            .addOnSuccessListener {
                Log.d(TAG, "Advertising started")
            }
            .addOnFailureListener {
                Log.e(TAG, "Advertising failed", it)
            }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Endpoint found: $endpointId")
            val endpoint = DiscoveredEndpoint(endpointId, info.endpointName, true)
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseHelper.saveDiscoveredEndpoint(endpoint)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, "", false))
            }
        }
    }

    fun sendMessageToEndpoint(endpointId: String, message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            val endpoint = DatabaseHelper.getDiscoveredEndpointById(endpointId)
            if (endpoint != null && endpoint.available) {
                connectToEndpoint(endpointId) {
                    sendPayload(endpointId, message)
                    disconnectFromEndpoint(endpointId)
                }
            } else {
                Log.e(TAG, "Endpoint $endpointId not available")
            }
        }
    }

    fun sendMessageToAllEndpoints(message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            val endpoints = DatabaseHelper.getAllDiscoveredEndpoints()
            endpoints.forEach { endpoint ->
                if (endpoint.available) {
                    connectToEndpoint(endpoint.id) {
                        sendPayload(endpoint.id, message)
                        disconnectFromEndpoint(endpoint.id)
                    }
                }
            }
        }
    }

    private fun connectToEndpoint(endpointId: String, onSuccess: () -> Unit) {
        Nearby.getConnectionsClient(context)
            .requestConnection(currentUserPhoneNumber, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Connection requested: $endpointId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e(TAG, "Connection request failed: $endpointId", it)
            }
    }

    private fun disconnectFromEndpoint(endpointId: String) {
        Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId)
        Log.d(TAG, "Disconnected from endpoint: $endpointId")
    }

    private fun sendPayload(endpointId: String, message: Message) {
        val payload = Payload.fromBytes(serializeMessage(message))
        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Payload sent to: $endpointId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Payload send failed to: $endpointId", it)
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(context)
                .acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Log.d(TAG, "Connection accepted: $endpointId")
                }
                .addOnFailureListener {
                    Log.e(TAG, "Connection acceptance failed: $endpointId", it)
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d(TAG, "Connected to endpoint: $endpointId")
                CoroutineScope(Dispatchers.Main).launch {
                    DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, "", false))
                }
            } else {
                Log.e(TAG, "Connection failed: $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, "", true))
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val receivedBytes = payload.asBytes()!!
                val message = deserializeMessage(receivedBytes)
                handleMessage(message)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Handle updates if needed
        }
    }

    private fun handleMessage(message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            DatabaseHelper.saveMessage(message)
            if (message.recipient == currentUserPhoneNumber) {
                Log.d(TAG, "Message received: ${message.content}")
                // Display or handle the message as needed
            } else {
                if (message.relays.size < 10) {
                    message.relays.add(currentUserPhoneNumber)
                    sendMessageToAllEndpoints(message)
                } else {
                    Log.d(TAG, "Message relay limit reached")
                }
            }
        }
    }

    /*fun sendMessageToRecipient(recipientPhoneNumber: String, content: String, currentUserPhoneNumber: String) {
        val message = Message(
            sender = currentUserPhoneNumber,
            recipient = recipientPhoneNumber,
            content = content,
            timestamp = System.currentTimeMillis(),
            relays = mutableListOf()
        )

        // Rechercher l'endpointId correspondant au numéro de téléphone du destinataire
        val recipientEndpoint = DatabaseHelper.getDiscoveredEndpointByName(recipientPhoneNumber)

        if (recipientEndpoint != null) {
            // Si un endpointId est trouvé, envoyer le message à cet endpoint
            sendMessageToEndpoint(recipientEndpoint.id, message)
        } else {
            // Si aucun endpointId n'est trouvé, envoyer le message à tous les endpoints
            sendMessageToAllEndpoints(message)
        }
    }*/

    fun sendMessageToRecipient(message: Message, param: (Any) -> Unit) {
        // Rechercher l'endpointId correspondant au numéro de téléphone du destinataire
        CoroutineScope(Dispatchers.Main).launch {
            val recipientEndpoint = DatabaseHelper.getDiscoveredEndpointByName(message.recipient)

            if (recipientEndpoint != null) {
                // Si un endpointId est trouvé, envoyer le message à cet endpoint
                sendMessageToEndpoint(recipientEndpoint.id, message)
            } else {
                // Si aucun endpointId n'est trouvé, envoyer le message à tous les endpoints
                sendMessageToAllEndpoints(message)
            }
        }
    }

    private val gson = Gson()

    private fun serializeMessage(message: Message): ByteArray {
        // Serialize the message object to a byte array using JSON
        val json = gson.toJson(message)
        return json.toByteArray(Charsets.UTF_8)
    }

    private fun deserializeMessage(data: ByteArray): Message {
        // Deserialize the byte array back to a message object using JSON
        val json = String(data, Charsets.UTF_8)
        return gson.fromJson(json, Message::class.java)
    }
}
