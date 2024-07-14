package com.example.tam_tam

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tam_tam.activities.ContactsActivity
import com.example.tam_tam.activities.CreateAccountActivity

class MainActivity : AppCompatActivity() {

    // Enregistre une demande de multiples permissions et définit un callback pour gérer les résultats
    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Si l'une des permissions n'est pas accordée, afficher un message et terminer l'activité
        if (permissions.entries.any { !it.value }) {
            Toast.makeText(this, "Required permissions needed", Toast.LENGTH_LONG).show()
            finish()
        } else {
            // Sinon, recrée l'activité
            recreate()
        }
    }

    // Vérifie les permissions nécessaires au démarrage de l'activité
    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestMultiplePermissions.launch(REQUIRED_PERMISSIONS)
        }
    }

    // Vérifie si toutes les permissions nécessaires sont accordées
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.isEmpty() || permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Initialisation de l'activité principale
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestMultiplePermissions.launch(REQUIRED_PERMISSIONS)
        } else {
            UserDatabaseHelper.init(this)
            checkUserAccount()
        }
    }

    // Vérifie s'il y a un compte utilisateur existant et lance l'activité appropriée
    private fun checkUserAccount() {
        val user = UserDatabaseHelper.getAllUsers().firstOrNull()
        if (user != null) {
            // L'utilisateur a déjà un compte, lance l'activité ContactsActivity
            val intent = Intent(this, ContactsActivity::class.java)
            val senderPhoneNumber = UserDatabaseHelper.getAllUsers().firstOrNull()?.phoneNumber ?: ""
            NearbyService.start(this, senderPhoneNumber)
            startActivity(intent)
            finish()
        } else {
            // Aucun compte utilisateur trouvé, lance l'activité CreateAccountActivity
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Définit les permissions requises en fonction de la version Android
    private companion object {
        val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
    }
}
