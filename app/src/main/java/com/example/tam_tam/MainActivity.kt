package com.example.tam_tam

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tam_tam.activities.ContactsActivity
import com.example.tam_tam.activities.CreateAccountActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UserDatabaseHelper.init(this)

        checkUserAccount()
    }

    private fun checkUserAccount() {
        val user = UserDatabaseHelper.getAllUsers().firstOrNull()
        if (user != null) {
            // User already has an account, proceed to ContactsActivity
            val intent = Intent(this, ContactsActivity::class.java)
            val senderPhoneNumber = UserDatabaseHelper.getAllUsers().firstOrNull()?.phoneNumber ?: ""
            NearbyService.start(this, senderPhoneNumber)
            startActivity(intent)
            finish()
        } else {
            // No user account found, proceed to CreateAccountActivity
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
