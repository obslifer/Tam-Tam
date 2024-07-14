package com.example.tam_tam.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class User(
    @PrimaryKey
    var phoneNumber: String = "",

    @Required
    var name: String = ""
) : RealmObject() {
    // Additional methods or properties can be added as needed
}