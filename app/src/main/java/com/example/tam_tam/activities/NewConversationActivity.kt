package com.example.tam_tam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.tam_tam.DatabaseHelper
import com.example.tam_tam.R
import com.example.tam_tam.UserDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewConversationActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnStartConversation: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_conversation)

        UserDatabaseHelper.init(this)

        etName = findViewById(R.id.etName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnStartConversation = findViewById(R.id.btnStartConversation)

        btnStartConversation.setOnClickListener {
            val name = etName.text.toString()
            val phoneNumber = etPhoneNumber.text.toString()

            if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                // Save the new contact to the database
                CoroutineScope(Dispatchers.Main).launch {
                    DatabaseHelper.saveContact(name = name, phoneNumber = phoneNumber)
                }

                // Start ChatActivity with the new contact's phone number
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("contactPhoneNumber", phoneNumber)
                startActivity(intent)
                finish()
            }
        }
    }
}
