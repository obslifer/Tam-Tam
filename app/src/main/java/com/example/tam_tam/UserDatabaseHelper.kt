package com.example.tam_tam

import android.content.Context
import android.os.AsyncTask
import com.example.tam_tam.models.User
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where

object UserDatabaseHelper {

    fun init(context: Context) {
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name("users.realm")
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(config)
    }

    fun saveUser(user: User) {
        AsyncTask.execute {
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction {
                it.insertOrUpdate(user)
            }
            realm.close()
        }
    }

    fun getUserByPhoneNumber(phoneNumber: String): User? {
        val realm = Realm.getDefaultInstance()
        val result = realm.where<User>().equalTo("phoneNumber", phoneNumber).findFirst()
        val user = realm.copyFromRealm(result)
        realm.close()
        return user
    }

    fun getUserPhoneNumber(name: String): String {
        val realm = Realm.getDefaultInstance()
        val result = realm.where<User>().equalTo("name", name).findFirst()
        val user = realm.copyFromRealm(result)
        realm.close()
        if (user != null) {
            return user.phoneNumber
        }
        return ""
    }

    fun getAllUsers(): List<User> {
        val realm = Realm.getDefaultInstance()
        val results = realm.where<User>().findAll()
        val users = realm.copyFromRealm(results)
        realm.close()
        return users
    }

    fun deleteUser(phoneNumber: String) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val result = it.where<User>().equalTo("phoneNumber", phoneNumber).findFirst()
            result?.deleteFromRealm()
        }
        realm.close()
    }
}