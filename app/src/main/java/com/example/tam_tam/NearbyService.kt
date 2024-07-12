package com.example.tam_tam

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.tam_tam.models.Message
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object NearbyService {
    private const val TAG = "NearbyService"
    private const val SERVICE_ID = "com.example.tam_tam"
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
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
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
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
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

    fun stopAdvertising() {
        Nearby.getConnectionsClient(context).stopAdvertising()
    }

    fun stopDiscovery() {
        Nearby.getConnectionsClient(context).stopDiscovery()
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Endpoint found: $endpointId")
            val endpoint = DiscoveredEndpoint(endpointId, info.endpointName, true)
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseHelper.saveDiscoveredEndpoint(endpoint)
                connectToEndpointWithRetry(endpointId, 3)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            CoroutineScope(Dispatchers.Main).launch {
                val endpoint = DatabaseHelper.getDiscoveredEndpointById(endpointId)
                if (endpoint != null) {
                    DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, endpoint.name, false))
                }
            }
        }
    }

    fun sendMessageToEndpoint(endpointId: String, message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            val endpoint = DatabaseHelper.getDiscoveredEndpointById(endpointId)
            if (endpoint != null && endpoint.available) {
                sendPayloadWithRetry(endpointId, message, 3)
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
                    sendPayloadWithRetry(endpoint.id, message, 3)
                }
            }
        }
    }

    private fun connectToEndpointWithRetry(endpointId: String, retries: Int) {
        if (retries == 0) {
            Log.e(TAG, "Connection failed after retries: $endpointId")
            Toast.makeText(context, "Connection failed: $endpointId", Toast.LENGTH_SHORT).show()
            return
        }

        Nearby.getConnectionsClient(context)
            .requestConnection(currentUserPhoneNumber, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Connection requested: $endpointId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Connection request failed: $endpointId", it)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000) // Wait for 1 second before retrying
                    connectToEndpointWithRetry(endpointId, retries - 1)
                }
            }
    }

    private fun sendPayloadWithRetry(endpointId: String, message: Message, retries: Int) {
        if (retries == 0) {
            Log.e(TAG, "Payload send failed after retries: $endpointId")
            Toast.makeText(context, "Message send failed: $endpointId", Toast.LENGTH_SHORT).show()
            startDiscovery()
            return
        }

        val payload = Payload.fromBytes(serializeMessage(message))
        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Payload sent to: $endpointId")
                CoroutineScope(Dispatchers.Main).launch {
                    DatabaseHelper.saveMessage(message)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Payload send failed to: $endpointId", it)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000) // Wait for 1 second before retrying
                    sendPayloadWithRetry(endpointId, message, retries - 1)
                }
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
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000) // Wait for 1 second before retrying
                        connectToEndpointWithRetry(endpointId, 3)
                    }
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d(TAG, "Connected to endpoint: $endpointId")
                CoroutineScope(Dispatchers.Main).launch {
                    val endpoint = DatabaseHelper.getDiscoveredEndpointById(endpointId)
                    if (endpoint != null) {
                        DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, endpoint.name, true))
                    }
                }
            } else {
                Log.e(TAG, "Connection failed: $endpointId")
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000) // Wait for 1 second before retrying
                    connectToEndpointWithRetry(endpointId, 3)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            CoroutineScope(Dispatchers.Main).launch {
                val endpoint = DatabaseHelper.getDiscoveredEndpointById(endpointId)
                if (endpoint != null) {
                    DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, endpoint.name, false))
                }
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
            // Save message
            DatabaseHelper.saveMessage(message)

            // Check if the message is for the current user
            if (message.recipient == currentUserPhoneNumber) {
                Log.d(TAG, "Message received: ${message.content}")

                // Check if sender is in contacts
                val contact = DatabaseHelper.getContact(message.sender)
                if (contact == null) {
                    // If not in contacts, save as unknown contact
                    DatabaseHelper.saveContact(message.sender, "Inconnu")
                }

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

    fun sendMessageToRecipient(message: Message, param: (Any) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val recipientEndpoint = DatabaseHelper.getDiscoveredEndpointByName(message.recipient)
            if (recipientEndpoint != null) {
                sendMessageToEndpoint(recipientEndpoint.id, message)
            } else {
                sendMessageToAllEndpoints(message)
            }
        }
    }

    private val gson = Gson()

    private fun serializeMessage(message: Message): ByteArray {
        val json = gson.toJson(message)
        return json.toByteArray(Charsets.UTF_8)
    }

    private fun deserializeMessage(data: ByteArray): Message {
        val json = String(data, Charsets.UTF_8)
        return gson.fromJson(json, Message::class.java)
    }
}
