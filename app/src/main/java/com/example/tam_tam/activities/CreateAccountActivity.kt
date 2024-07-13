package com.example.tam_tam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.tam_tam.DatabaseHelper
import com.example.tam_tam.R
import com.example.tam_tam.UserDatabaseHelper
import com.example.tam_tam.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var etPhoneNumber: EditText
    private lateinit var btnCreateAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        UserDatabaseHelper.init(this)

        // Initialize views
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)

        UserDatabaseHelper.init(this)

        btnCreateAccount.setOnClickListener {
            val phoneNumber = etPhoneNumber.text.toString()
            if (phoneNumber.isNotEmpty()) {
                val user = User(phoneNumber = phoneNumber, name = "User")
                UserDatabaseHelper.saveUser(user)

                val intent = Intent(this, ContactsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
