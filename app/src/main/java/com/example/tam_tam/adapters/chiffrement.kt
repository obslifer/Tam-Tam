package com.example.tam_tam.adapters

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object CryptoUtil {

    // Algorithme et Transformation de chiffrement utilisé
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES"

    /**
     * Génère une clé secrète basée sur le numéro de l'appareil pour le chiffrement AES-128.
     * La clé est dérivée du numéro de l'appareil et est tronquée ou complétée pour faire 16 octets.
     *
     * @param deviceNumber Numéro de l'appareil utilisé pour générer la clé.
     * @return Clé secrète générée pour le chiffrement.
     */
    fun generateKey(deviceNumber: String): SecretKey {
        val key = deviceNumber.toByteArray(Charsets.UTF_8).copyOf(16)
        return SecretKeySpec(key, ALGORITHM)
    }

    /**
     * Chiffre les données en utilisant la clé secrète fournie.
     *
     * @param data Données à chiffrer (texte en clair).
     * @param key Clé secrète utilisée pour le chiffrement.
     * @return Données chiffrées sous forme de chaîne encodée en Base64.
     */
    fun encrypt(data: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    /**
     * Déchiffre les données chiffrées en utilisant la clé secrète fournie.
     *
     * @param data Données chiffrées encodées en Base64.
     * @param key Clé secrète utilisée pour le déchiffrement.
     * @return Données déchiffrées sous forme de chaîne.
     */
    fun decrypt(data: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decryptedBytes = cipher.doFinal(Base64.decode(data, Base64.DEFAULT))
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
