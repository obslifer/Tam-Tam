package com.example.tam_tam.models

import com.example.tam_tam.NearbyService
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.UUID

open class RealmMessage(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var sender: String = "",
    var recipient: String = "",
    var content: String = "",
    var timestamp: Long = 0L,
    var relays: String = ""
) : RealmObject() {
    fun toMessage(): Message {
        return Message(sender, recipient, content, timestamp, relays.split(",").toMutableList())
    }

    companion object {
        fun fromMessage(message: Message): RealmMessage {
            return RealmMessage(
                id = UUID.randomUUID().toString(),
                sender = message.sender,
                recipient = message.recipient,
                content = message.content,
                timestamp = message.timestamp,
                relays = message.relays.joinToString(",")
            )
        }
    }
}

open class Contact(
    @PrimaryKey var phoneNumber: String = "",
    var name: String = ""
) : RealmObject()

open class RealmDiscoveredEndpoint(
    var id: String = "",
    @PrimaryKey var name: String = "",
    var available: Boolean = true
) : RealmObject() {
    fun toDiscoveredEndpoint(): NearbyService.DiscoveredEndpoint {
        return NearbyService.DiscoveredEndpoint(id, name, available)
    }

    companion object {
        fun fromDiscoveredEndpoint(endpoint: NearbyService.DiscoveredEndpoint): RealmDiscoveredEndpoint {
            return RealmDiscoveredEndpoint(endpoint.id, endpoint.name, endpoint.available)
        }
    }
}