package com.example.tam_tam

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tam_tam.NearbyConnection.ConnectionManager

class MainActivity : AppCompatActivity() {
    // Déclaration de l'instance de ConnectionManager
    private lateinit var connectionManager: ConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation de ConnectionManager
        connectionManager = ConnectionManager(this)
        val endpointName = generateUniqueEndpointName()
        // Démarrage de la publicité et de la découverte
        connectionManager.startAdvertisingAndDiscovery(endpointName)
    }

    // Génération d'un nom unique pour chaque appareil
    private fun generateUniqueEndpointName(): String {
        // Implémentez une logique pour générer un nom unique pour chaque appareil
        return "Device_${System.currentTimeMillis()}"
    }
}