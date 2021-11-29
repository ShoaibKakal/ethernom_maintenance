package com.ethernom.maintenance

import android.os.Build
import androidx.annotation.RequiresApi
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class ECDSA
{
    var publicKey: String = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAERqWa2kD1HGYRLS4CAhQRTnEqtBxqdK9x+U+/Wu98Gcw3S2vyu1tz5OuEBiPObbHKiOVxorGIIXbGEn2macknqA=="
//    var publicKey: String = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE+HCPZzC71W2jZhzb2KGDi6bintaZhYxLk62uPuYVRNvbrDWsaaTeJ7VHFODJSkxhxcvHJYyGJrwu5LkS7gAbVQ=="
    var privateKey: String = "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgRF7s79SH2eF3rO8/mzPDVGbjh/I8bw9kFt0S+rLFNdigBwYFK4EEAAqhRANCAAT4cI9nMLvVbaNmHNvYoYOLpuKe1pmFjEuTra4+5hVE29usNaxppN4ntUcU4MlKTGHFy8cljIYmvC7kuRLuABtV"

    fun generateKeyPair() {
        val ecSpec = ECGenParameterSpec("secp256k1")
        val g: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
        g.initialize(ecSpec, SecureRandom())
        val keypair: KeyPair = g.generateKeyPair()
//         publicKey = keypair.public
//         privateKey =keypair.private
    }

    fun sign (msg:String) :String{
        //at sender's end
        val ecdsaSign = Signature.getInstance("SHA256withECDSA")

        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey))
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(privateKeySpec)

        ecdsaSign.initSign(privateKey)
        ecdsaSign.update(msg.encodeToByteArray())
        val signature = ecdsaSign.sign()
        return Base64.getEncoder().encodeToString(signature)
    }

    fun verify(publicKey: String, msg: String, signature: String):Boolean {
        val ecdsaVerify = Signature.getInstance("SHA256withECDSA")

        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))

        val keyFactory = KeyFactory.getInstance("EC")
        val publicKey = keyFactory.generatePublic(publicKeySpec)

        ecdsaVerify.initVerify(publicKey)
        ecdsaVerify.update(msg.encodeToByteArray())
        return ecdsaVerify.verify(Base64.getDecoder().decode(signature))
    }

}