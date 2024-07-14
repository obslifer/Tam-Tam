package com.example.tam_tam

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.tam_tam.models.Message
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

@SuppressLint("StaticFieldLeak")
object NearbyService {
    private const val TAG = "NearbyService"
    private const val SERVICE_ID = "com.example.tam_tam"
    private lateinit var context: Context
    private lateinit var currentUserPhoneNumber: String
    private val incomingFilePayloads = mutableMapOf<Long, String>()
    val discoveredEndpoints = mutableListOf<DiscoveredEndpoint>()

    data class DiscoveredEndpoint(val id: String, val name: String, var available: Boolean)

    // Initialise le service Nearby avec le contexte et le numéro de téléphone de l'utilisateur actuel
    fun start(context: Context, phoneNumber: String) {
        this.context = context.applicationContext
        this.currentUserPhoneNumber = phoneNumber
        DatabaseHelper.init(context)
        startDiscovery()
        startAdvertising()
    }

    // Démarre la découverte des endpoints Nearby
    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                Log.d(TAG, "Discovery started")
            }
            .addOnFailureListener {
                Log.e(TAG, "Discovery failed", it)
            }
    }

    // Démarre l'annonce des endpoints Nearby
    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
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

    // Arrête l'annonce des endpoints Nearby
    fun stopAdvertising() {
        Nearby.getConnectionsClient(context)
            .stopAdvertising()
    }

    // Callback pour la découverte d'un endpoint Nearby
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Endpoint found: $endpointId")
            val endpoint = DiscoveredEndpoint(endpointId, info.endpointName, true)
            discoveredEndpoints.add(endpoint)
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseHelper.saveDiscoveredEndpoint(endpoint)
                connectToEndpointWithRetry(endpointId, 3)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            discoveredEndpoints.removeAll { it.id == endpointId }
            CoroutineScope(Dispatchers.Main).launch {
                val endpoint = DatabaseHelper.getDiscoveredEndpointById(endpointId)
                if (endpoint != null) {
                    DatabaseHelper.saveDiscoveredEndpoint(DiscoveredEndpoint(endpointId, endpoint.name, false))
                }
            }
        }
    }

    // Envoie un message à un endpoint spécifique
    fun sendMessageToEndpoint(endpointId: String, message: Message, deviceNumber: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val endpoint = discoveredEndpoints.find { it.id == endpointId }
            if (endpoint != null && endpoint.available) {
                sendPayloadWithRetry(endpointId, message, deviceNumber, 3)
            } else {
                Log.e(TAG, "Endpoint $endpointId not available")
            }
        }
    }

    // Envoie un message à tous les endpoints découverts
    fun sendMessageToAllEndpoints(message: Message, deviceNumber: String) {
        CoroutineScope(Dispatchers.Main).launch {
            discoveredEndpoints.forEach { endpoint ->
                if (!message.relays.contains(endpoint.name)) {
                    if (endpoint.available) {
                        val payload = Payload.fromBytes(serializeMessage(message))
                        sendPayloadWithRetry(
                            endpoint.id,
                            payload,
                            deviceNumber,
                            2
                        )
                    }
                }
            }
        }
    }

    // Connexion à un endpoint avec possibilité de retry
    private fun connectToEndpointWithRetry(endpointId: String, retryCount: Int) {
        if (retryCount <= 0) {
            Log.e(TAG, "Connection retries exhausted for endpoint: $endpointId")
            Toast.makeText(context, "Failed to connect to endpoint: $endpointId", Toast.LENGTH_SHORT).show()
            return
        }

        Nearby.getConnectionsClient(context)
            .requestConnection(currentUserPhoneNumber, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Connection requested: $endpointId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Connection request failed: $endpointId", it)
                connectToEndpointWithRetry(endpointId, retryCount - 1)
            }
    }

    // Envoie un payload avec possibilité de retry
    private fun sendPayloadWithRetry(endpointId: String, message: Message, deviceNumber: String, retryCount: Int) {
        if (retryCount <= 0) {
            Log.e(TAG, "Payload send retries exhausted for endpoint: $endpointId")
            Toast.makeText(context, "Failed to send message to endpoint: $endpointId", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = Payload.fromBytes(serializeMessage(message))
        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Payload sent to: $endpointId")
                CoroutineScope(Dispatchers.Main).launch {
                    DatabaseHelper.saveMessage(message, deviceNumber)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Payload send failed to: $endpointId", it)
                sendPayloadWithRetry(endpointId, message, deviceNumber, retryCount - 1)
            }
    }

    // Callback pour le cycle de vie de la connexion Nearby
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
                    val endpoint = discoveredEndpoints.find { it.id == endpointId }
                    if (endpoint != null) {
                        endpoint.available = true
                        DatabaseHelper.saveDiscoveredEndpoint(endpoint)
                        // Envoyer tous les messages qui ne sont pas pour l'utilisateur actuel
                        sendAllMessagesToEndpoint(endpoint.id)
                    }
                }
            } else {
                Log.e(TAG, "Connection failed: $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            CoroutineScope(Dispatchers.Main).launch {
                val endpoint = discoveredEndpoints.find { it.id == endpointId }
                if (endpoint != null) {
                    endpoint.available = false
                    DatabaseHelper.saveDiscoveredEndpoint(endpoint)
                }
            }
        }
    }

    // Callback pour les payloads reçus
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val receivedBytes = payload.asBytes()!!
                try {
                    val message = deserializeMessage(receivedBytes)
                    handleMessage(message)
                } catch (e: Exception) {
                    Log.e("error", e.toString())
                    handleBytesPayload(endpointId, payload)
                }
            } else if (payload.type == Payload.Type.FILE) {
                handleFilePayload(endpointId, payload)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Gérer les mises à jour si nécessaire
        }
    }

    // Gère un message reçu
    private fun handleMessage(message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            // Sauvegarde le message
            DatabaseHelper.saveMessage(message, currentUserPhoneNumber)

            // Vérifie si le message est destiné à l'utilisateur actuel
            if (message.recipient == currentUserPhoneNumber) {
                Log.d(TAG, "Message received: ${message.content}")

                // Vérifie si l'expéditeur est dans les contacts
                val contact = DatabaseHelper.getContact(message.sender)
                if (contact == null) {
                    // Si l'expéditeur n'est pas dans les contacts, enregistre comme un contact inconnu
                    Log.d(TAG, "Message from unknown received: ${message.content}")
                    DatabaseHelper.saveContact(message.sender, "Inconnu")
                }

                // Affiche ou traite le message selon les besoins
            } else {
                if (message.relays.size < 10) {
                    message.relays.add(currentUserPhoneNumber)
                    sendMessageToAllEndpoints(message, currentUserPhoneNumber)
                } else {
                    Log.d(TAG, "Message relay limit reached")
                }
            }
        }
    }

    // Gère un payload de type bytes
    private fun handleBytesPayload(endpointId: String, payload: Payload) {
        val message = String(payload.asBytes()!!, StandardCharsets.UTF_8)
        val parts = message.split(":")
        if (parts.size == 2) {
            val payloadId = parts[0].toLong()
            val fileName = parts[1]
            Log.d(TAG, "Received filename message: $fileName with payload ID: $payloadId")
            incomingFilePayloads[payloadId] = fileName
        }
    }

    // Gère un payload de type file
    private fun handleFilePayload(endpointId: String, payload: Payload) {
        val file = payload.asFile()!!.asParcelFileDescriptor().fileDescriptor
        val payloadId = payload.id
        if (incomingFilePayloads.containsKey(payloadId)) {
            val fileName = incomingFilePayloads[payloadId]
            Log.d(TAG, "Received file: $fileName")
            saveReceivedFile(file, fileName!!)
        }
    }

    // Sauvegarde le fichier reçu
    private fun saveReceivedFile(file: FileDescriptor, fileName: String) {
        val inputStream = FileInputStream(file)
        val outputFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(outputFile)

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()

        val fileUri = Uri.fromFile(outputFile)
        Log.d(TAG, "Saved received file to: $fileUri")
    }

    // Envoie un message à un destinataire spécifique
    fun sendMessageToRecipient(message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            val recipientEndpoint = discoveredEndpoints.find { it.name == message.recipient }
            if (recipientEndpoint != null && recipientEndpoint.available) {
                sendMessageToEndpoint(recipientEndpoint.id, message, currentUserPhoneNumber)
            } else {
                sendMessageToAllEndpoints(message, currentUserPhoneNumber)
            }
        }
    }

    // Envoie tous les messages à un endpoint spécifique
    private fun sendAllMessagesToEndpoint(endpointId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val messages = DatabaseHelper.getMessages()
            messages.forEach { message ->
                val endpoint = discoveredEndpoints.find { it.id == endpointId }
                if (endpoint != null) {
                    if (message.recipient != currentUserPhoneNumber && !message.relays.contains(endpoint.name) && message.sender != endpoint.name && message.recipient != endpoint.name) {
                        sendPayloadWithRetry(
                            endpointId,
                            Payload.fromBytes(serializeMessage(message)),
                            currentUserPhoneNumber,
                            2
                        )
                    }
                }
            }
        }
    }

    // Gson pour la sérialisation/désérialisation JSON
    private val gson = Gson()

    // Sérialise un message en tableau de bytes
    private fun serializeMessage(message: Message): ByteArray {
        val json = gson.toJson(message)
        return json.toByteArray(Charsets.UTF_8)
    }

    // Désérialise un tableau de bytes en objet Message
    private fun deserializeMessage(bytes: ByteArray): Message {
        val json = bytes.toString(Charsets.UTF_8)
        return gson.fromJson(json, Message::class.java)
    }

    // Envoie une image à un destinataire spécifique
    fun sendImageToRecipient(uri: Uri, recipient: String) {
        val endpoint = discoveredEndpoints.find { it.name == recipient }
        if (endpoint != null) {
            sendImageToEndpoint(endpoint.id, uri, recipient)
        } else {
            Log.e(TAG, "No endpoint found for recipient: $recipient")
        }
    }

    // Envoie une image à un endpoint spécifique
    private fun sendImageToEndpoint(endpointId: String, uri: Uri, recipient: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            val filePayload = pfd?.let { Payload.fromFile(it) }
            val filenameMessage = filePayload?.id.toString() + ":" + uri.lastPathSegment
            val filenameBytesPayload = Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))

            sendPayloadWithRetry(endpointId, filenameBytesPayload, recipient, 3)

            if (filePayload != null) {
                sendPayloadWithRetry(endpointId, filePayload, recipient, 3)
                val message = Message(
                    sender = currentUserPhoneNumber,
                    recipient = recipient,
                    content = "",
                    timestamp = System.currentTimeMillis(),
                    relays = mutableListOf(),
                    imageUri = uri.toString()
                )
                sendMessageToRecipient(message)
            }
        }
    }

    // Envoie un payload avec possibilité de retry
    private fun sendPayloadWithRetry(endpointId: String, payload: Payload, recipient: String, retryCount: Int) {
        if (retryCount <= 0) {
            Log.e(TAG, "Payload send retries exhausted for endpoint: $endpointId")
            Toast.makeText(context, "Failed to send payload to endpoint: $endpointId", Toast.LENGTH_SHORT).show()
            return
        }

        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Payload sent to: $endpointId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Payload send failed to: $endpointId", it)
                sendPayloadWithRetry(endpointId, payload, recipient, retryCount - 1)
            }
    }
}
