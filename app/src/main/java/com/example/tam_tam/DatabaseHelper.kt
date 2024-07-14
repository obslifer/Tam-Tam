package com.example.tam_tam

import android.content.Context
import android.util.Log
import com.example.tam_tam.adapters.CryptoUtil
import com.example.tam_tam.models.Contact
import com.example.tam_tam.models.Message
import com.example.tam_tam.models.RealmDiscoveredEndpoint
import com.example.tam_tam.models.RealmMessage
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

object DatabaseHelper {

    // Initialise Realm avec le contexte donné et définit la configuration par défaut
    fun init(context: Context) {
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name("tam_tam.realm")
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(config)
    }

    // Enregistre un message dans la base de données Realm, en cryptant d'abord le contenu
    suspend fun saveMessage(message: Message, deviceNumber: String) {
        withContext(Dispatchers.IO) {
            val key: SecretKey = CryptoUtil.generateKey(deviceNumber)
            val encryptedContent = CryptoUtil.encrypt(message.content, key)
            val realmMessage = RealmMessage.fromMessage(message.copy(content = encryptedContent))
            val realm = Realm.getDefaultInstance()
            Log.e("save image", "message: $realmMessage")
            realm.executeTransaction { it.insertOrUpdate(realmMessage) }
            realm.close()
        }
    }

    // Récupère tous les messages de la base de données Realm
    suspend fun getMessages(): List<Message> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val results = realm.where<RealmMessage>().findAll()
            val messages = realm.copyFromRealm(results).map { it.toMessage() }
            realm.close()
            messages
        }
    }

    // Récupère un message par son ID, en décryptant le contenu
    suspend fun getMessageById(id: String, deviceNumber: String): Message? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<RealmMessage>().equalTo("id", id).findFirst()
            val message = result?.toMessage()
            realm.close()
            message?.let {
                val key: SecretKey = CryptoUtil.generateKey(deviceNumber)
                val decryptedContent = CryptoUtil.decrypt(it.content, key)
                it.copy(content = decryptedContent)
            }
        }
    }

    // Récupère les messages pour une conversation spécifique entre deux numéros de téléphone
    suspend fun getMessagesForConversation(senderPhoneNumber: String, recipientPhoneNumber: String): List<Message> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val results = realm.where<RealmMessage>()
                .beginGroup()
                .equalTo("sender", senderPhoneNumber)
                .and()
                .equalTo("recipient", recipientPhoneNumber)
                .endGroup()
                .or()
                .beginGroup()
                .equalTo("sender", recipientPhoneNumber)
                .and()
                .equalTo("recipient", senderPhoneNumber)
                .endGroup()
                .findAll()
            val messages = realm.copyFromRealm(results).map { it.toMessage() }
            realm.close()
            Log.e("list of messages", "messages: $messages")
            messages
        }
    }

    // Supprime un message par son ID de la base de données Realm
    suspend fun deleteMessage(id: String) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                val result = it.where<RealmMessage>().equalTo("id", id).findFirst()
                result?.deleteFromRealm()
            }
            realm.close()
        }
    }

    // Enregistre un contact dans la base de données Realm, en le mettant à jour s'il existe déjà
    suspend fun saveContact(phoneNumber: String, name: String) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val existingContact = realm.where(Contact::class.java).equalTo("phoneNumber", phoneNumber).findFirst()

            realm.executeTransaction { transactionRealm ->
                if (existingContact != null) {
                    // Met à jour le nom du contact existant
                    existingContact.name = name
                } else {
                    // Crée un nouveau contact s'il n'existe pas
                    val newContact = Contact(phoneNumber, name)
                    transactionRealm.insertOrUpdate(newContact)
                }
            }
            realm.close()
        }
    }

    // Récupère un contact par son numéro de téléphone de la base de données Realm
    suspend fun getContact(phoneNumber: String): Contact? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<Contact>().equalTo("phoneNumber", phoneNumber).findFirst()
            val contact = result?.let { realm.copyFromRealm(it) }
            realm.close()
            contact
        }
    }

    // Récupère tous les contacts de la base de données Realm
    suspend fun getAllContacts(): List<Contact> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val results = realm.where<Contact>().findAll()
            val contacts = realm.copyFromRealm(results)
            realm.close()
            contacts
        }
    }

    // Enregistre un endpoint découvert dans la base de données Realm
    suspend fun saveDiscoveredEndpoint(endpoint: NearbyService.DiscoveredEndpoint) {
        withContext(Dispatchers.IO) {
            val realmEndpoint = RealmDiscoveredEndpoint.fromDiscoveredEndpoint(endpoint)
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction { it.insertOrUpdate(realmEndpoint) }
            realm.close()
        }
    }

    // Récupère un endpoint découvert par son ID de la base de données Realm
    suspend fun getDiscoveredEndpointById(id: String): NearbyService.DiscoveredEndpoint? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<RealmDiscoveredEndpoint>().equalTo("id", id).findFirst()
            val endpoint = result?.toDiscoveredEndpoint()
            realm.close()
            endpoint
        }
    }

    // Récupère un endpoint découvert par son nom de la base de données Realm
    suspend fun getDiscoveredEndpointByName(name: String): NearbyService.DiscoveredEndpoint? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<RealmDiscoveredEndpoint>().equalTo("name", name).findFirst()
            val endpoint = result?.toDiscoveredEndpoint()
            realm.close()
            endpoint
        }
    }

    // Récupère tous les endpoints découverts de la base de données Realm
    suspend fun getAllDiscoveredEndpoints(): List<NearbyService.DiscoveredEndpoint> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val results = realm.where<RealmDiscoveredEndpoint>().findAll()
            val endpoints = realm.copyFromRealm(results).map { it.toDiscoveredEndpoint() }
            realm.close()
            endpoints
        }
    }
}
