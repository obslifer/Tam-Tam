package com.example.tam_tam

import android.content.Context
import com.example.tam_tam.models.Contact
import com.example.tam_tam.models.Message
import com.example.tam_tam.models.RealmDiscoveredEndpoint
import com.example.tam_tam.models.RealmMessage
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun saveMessage(message: Message) {
        withContext(Dispatchers.IO) {
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

    suspend fun getMessageById(id: String): Message? {
        return withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            val result = realm.where<RealmMessage>().equalTo("id", id).findFirst()
            val message = result?.toMessage()
            realm.close()
            message
        }
    }

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
            messages
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
            val contact = Contact(phoneNumber, name)
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction { it.insertOrUpdate(contact) }
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
