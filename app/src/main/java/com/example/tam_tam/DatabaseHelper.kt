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

    fun init(context: Context) {
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name("tam_tam.realm")
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(config)
    }

    suspend fun saveMessage(message: Message, deviceNumber: String) {
        withContext(Dispatchers.IO) {
            val key: SecretKey = CryptoUtil.generateKey(deviceNumber)
            val encryptedContent = CryptoUtil.encrypt(message.content, key)
            //val realmMessage = RealmMessage.fromMessage(message.copy(content = encryptedContent))
            val realmMessage = RealmMessage.fromMessage(message)
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction { it.insertOrUpdate(realmMessage) }
            realm.close()
        }
    }

    suspend fun getMessages(): List<Message> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val results = realm.where<RealmMessage>().findAll()
            val messages = realm.copyFromRealm(results).map { it.toMessage() }
            realm.close()
            messages
        }
    }

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

    suspend fun getMessagesForConversation(senderPhoneNumber: String, recipientPhoneNumber: String, deviceNumber: String): List<Message> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
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
                val messages = realm.copyFromRealm(results).map {
                    try {
                        val key: SecretKey = CryptoUtil.generateKey(deviceNumber)
                        val decryptedContent = CryptoUtil.decrypt(it.content, key)
                        it.toMessage().copy(content = decryptedContent)
                    } catch (e: Exception) {
                        // Log or handle decryption error
                        Log.e("CryptoUtil", "Error decrypting message content", e)
                        it.toMessage() // Return as-is or handle error case
                    }
                }
                messages
            } finally {
                realm.close()
            }
        }
    }


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

    suspend fun saveContact(phoneNumber: String, name: String) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val existingContact = realm.where(Contact::class.java).equalTo("phoneNumber", phoneNumber).findFirst()

            realm.executeTransaction { transactionRealm ->
                if (existingContact != null) {
                    // Update the existing contact's name
                    existingContact.name = name
                } else {
                    // Create a new contact if it doesn't exist
                    val newContact = Contact(phoneNumber, name)
                    transactionRealm.insertOrUpdate(newContact)
                }
            }
            realm.close()
        }
    }

    suspend fun getContact(phoneNumber: String): Contact? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<Contact>().equalTo("phoneNumber", phoneNumber).findFirst()
            val contact = result?.let { realm.copyFromRealm(it) }
            realm.close()
            contact
        }
    }

    suspend fun getAllContacts(): List<Contact> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val results = realm.where<Contact>().findAll()
            val contacts = realm.copyFromRealm(results)
            realm.close()
            contacts
        }
    }

    suspend fun saveDiscoveredEndpoint(endpoint: NearbyService.DiscoveredEndpoint) {
        withContext(Dispatchers.IO) {
            val realmEndpoint = RealmDiscoveredEndpoint.fromDiscoveredEndpoint(endpoint)
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction { it.insertOrUpdate(realmEndpoint) }
            realm.close()
        }
    }

    suspend fun getDiscoveredEndpointById(id: String): NearbyService.DiscoveredEndpoint? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<RealmDiscoveredEndpoint>().equalTo("id", id).findFirst()
            val endpoint = result?.toDiscoveredEndpoint()
            realm.close()
            endpoint
        }
    }

    suspend fun getDiscoveredEndpointByName(name: String): NearbyService.DiscoveredEndpoint? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<RealmDiscoveredEndpoint>().equalTo("name", name).findFirst()
            val endpoint = result?.toDiscoveredEndpoint()
            realm.close()
            endpoint
        }
    }

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
