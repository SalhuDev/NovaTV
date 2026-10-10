package com.novatv.player

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Source URLs and credentials stay in private, Keystore-encrypted app storage. */
class Storage(context: Context, private val name: String = "nova") {
 private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
 private fun key(): SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  (store.getKey(if (name == "nova") "nova.sources" else "$name.key", null) as? SecretKey)?.let { return it }
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder(if (name == "nova") "nova.sources" else "$name.key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  }.generateKey()
 }
 fun save(value: JSONObject, durable: Boolean = false) {
  val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
  val encrypted = cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
  val edit = preferences.edit().putString("data", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP))
  if (durable) check(edit.commit()) { "Opslaan mislukt" } else edit.apply()
 }
 fun load(): JSONObject {
  val saved = preferences.getString("data", null) ?: return JSONObject()
  val bytes = Base64.decode(saved, Base64.NO_WRAP)
  require(bytes.size > 12)
  val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
  return JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
 }
}
