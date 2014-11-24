package services

import javax.crypto.{SecretKeyFactory, Cipher}
import javax.crypto.spec.{PBEKeySpec, SecretKeySpec}

import sun.misc.{BASE64Decoder, BASE64Encoder}

/**
 * Created by markmo on 4/11/2014.
 */
object CryptoService {

  final val passphrase = "changeme"
  final val salt = "ewf57w5fw!@#R"

  // RC4 is one of the fastest ciphers
  val key = new SecretKeySpec(passphrase.getBytes, "RC4")
  val cipher = Cipher.getInstance("RC4")
  val encoder = new BASE64Encoder
  val decoder = new BASE64Decoder

  val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
  val keySpec = new PBEKeySpec(passphrase.toCharArray, salt.getBytes, 65536, 128)
  val tmp = factory.generateSecret(keySpec)
  val secret = new SecretKeySpec(tmp.getEncoded, "AES")
  val blockCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

  def obfuscate(cleartext: String) = {
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val utf8 = cleartext.getBytes("UTF8")
    encoder.encode(cipher.doFinal(utf8))
  }

  def clarify(enc: String) = {
    cipher.init(Cipher.DECRYPT_MODE, key)
    new String(cipher.doFinal(decoder.decodeBuffer(enc)), "UTF8")
  }

  def blockObfuscate(cleartext: String) = {
    blockCipher.init(Cipher.ENCRYPT_MODE, secret)
    encoder.encode(blockCipher.doFinal(cleartext.getBytes))
  }

  def blockClarify(enc: String) = {
    blockCipher.init(Cipher.DECRYPT_MODE, secret)
    new String(blockCipher.doFinal(enc.getBytes))
  }

}
