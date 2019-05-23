package sobaya.app.security

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executor
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val executor = Executor { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val authenticatedCryptoObject: BiometricPrompt.CryptoObject? =
                        result.getCryptoObject()
                    Toast.makeText(applicationContext, authenticatedCryptoObject.hashCode().toString(), Toast.LENGTH_SHORT).show()
                    // User has verified the signature, cipher, or message
                    // authentication code (MAC) associated with the crypto object, so
                    // you can use it in your app's crypto-driven workflows.
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        // Displays the "log in" prompt.
        biometricPrompt.authenticate(promptInfo)
    }

    private fun pending() {
        val key = createKeyString()
        // 暗号化したSharedPreferencesを用意
        encrypt(key)
        // 普通のSharedPreferencesとして読んでみる
        read()
        // EncryptedSharedPreferencesとして読んでみる
        decrypt(key)
        // 普通のSharedPreferencesとして追記
        write()
        read()
        decrypt(key)
    }

    private fun createKeyString(): String {

        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val key = (1..255)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

        return key
    }

    private fun write() {
        val spf = getSharedPreferences("sobaya", Context.MODE_PRIVATE)
        spf.edit().putString("TEST2", "蕎麦は健康").commit()
    }

    private fun decrypt(key: String) {
        val keyGenParameterSpec = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val spec = KeyGenParameterSpec.Builder(key, KeyProperties.PURPOSE_ENCRYPT).build()
        val masterKeyAlias = MasterKeys.getOrCreate(spec)
        val spf = EncryptedSharedPreferences.create(
            "sobaya",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val test = spf.getString("TEST", "")
        val test2 = spf.getString("TEST2", "")
        System.out.println("READ: ${test} : ${test2}")
    }

    private fun read() {
        val spf = getSharedPreferences("sobaya", Context.MODE_PRIVATE)
        val test = spf.getString("TEST", "")
        val test2 = spf.getString("TEST2", "")
        System.out.println("READ: ${test} : ${test2}")
    }

    private fun encrypt(key: String) {
        val keyGenParameterSpec = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val spec = KeyGenParameterSpec.Builder(keyGenParameterSpec, KeyProperties.PURPOSE_SIGN).setKeySize(256).build()
        val masterKeyAlias = MasterKeys.getOrCreate(spec)
        val spf = EncryptedSharedPreferences.create(
            "sobaya",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val editor = spf.edit()

        editor.putString("TEST", "蕎麦は美味しいよ")
        editor.commit()
    }
}
