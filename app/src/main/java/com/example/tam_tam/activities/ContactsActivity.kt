package com.example.tam_tam.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tam_tam.DatabaseHelper
import com.example.tam_tam.NearbyService
import com.example.tam_tam.R
import com.example.tam_tam.adapters.ContactsAdapter
import com.example.tam_tam.models.User
import com.example.tam_tam.UserDatabaseHelper
import com.example.tam_tam.models.Contact
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabNewConversation: FloatingActionButton
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var contactsList: List<Contact>

    // Inside ContactsActivity class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        UserDatabaseHelper.init(this)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        fabNewConversation = findViewById(R.id.fabNewConversation)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        contactsAdapter = ContactsAdapter(mutableListOf(), { contact ->
            // Handle click action here, e.g., start ChatActivity
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("contactPhoneNumber", contact.phoneNumber)
            startActivity(intent)
        }, { contact ->
            // Handle long click action here, e.g., show modify contact dialog
            showModifyContactDialog(contact)
        })
        recyclerView.adapter = contactsAdapter

        // Set up FloatingActionButton
        fabNewConversation.setOnClickListener {
            val intent = Intent(this, NewConversationActivity::class.java)
            startActivity(intent)
        }

        // Load contacts from database
        loadContacts()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        loadContacts()
    }

    private fun showModifyContactDialog(contact: Contact) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Modifier le nom du contact")

        // Set up the input
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(contact.name) // Pré-remplir le champ avec le nom actuel du contact
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK") { dialog, _ ->
            val newContactName = input.text.toString().trim()
            if (newContactName.isNotEmpty()) {
                modifyContactName(contact.phoneNumber, newContactName)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Annuler") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun modifyContactName(contactPhoneNumber: String, newContactName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Update the contact name in the local database
            DatabaseHelper.saveContact(contactPhoneNumber, newContactName)

            // Reload contacts from database
            loadContacts()
        }
    }

    private fun loadContacts() {
        CoroutineScope(Dispatchers.Main).launch {
            val contacts = DatabaseHelper.getAllContacts()
            contactsAdapter.updateData(contacts)
            Log.d("ContactsActivity", "Contacts loaded: $contacts")
        }
    }

}
