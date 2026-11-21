package com.dosetrack.export

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class PasswordRequiredException : Exception("This backup is password protected.")

class WrongPasswordException : Exception("Wrong password, or the file is damaged.")

/**
 * Password-based encryption for backup files, using only platform crypto (no extra dependencies).
 *
 * File layout (all integers big-endian):
 * ```
 * "DTBK" | version (1 byte) | PBKDF2 iterations (4) | salt (16) | GCM IV (12) | AES-256-GCM ciphertext + 128-bit tag
 * ```
 * The key is PBKDF2-HMAC-SHA256(password, salt, iterations). The whole header is bound to the
 * ciphertext as GCM associated data, so tampering with it (e.g. lowering the iteration count)
 * makes decryption fail just like a wrong password.
 */
object BackupCrypto {

    const val FILE_EXTENSION = "dtbackup"
    const val MIME_TYPE = "application/octet-stream"

    /** OWASP's current recommendation for PBKDF2-HMAC-SHA256; stored per file so it can be raised later. */
    const val DEFAULT_ITERATIONS = 600_000

    private val MAGIC = byteArrayOf('D'.code.toByte(), 'T'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + IV_BYTES

    /** Bounds accepted when reading, so a crafted header can't make key derivation run for hours. */
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 10_000_000

    fun isEncrypted(data: ByteArray): Boolean =
        data.size >= MAGIC.size && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(
        plaintext: ByteArray,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC)
            .put(VERSION)
            .putInt(iterations)
            .put(salt)
            .put(iv)
            .array()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plaintext)
    }

    /** @throws WrongPasswordException if the password is wrong or the file was modified. */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (!isEncrypted(data) || data.size < HEADER_BYTES + TAG_BITS / 8) {
            throw InvalidImportFileException("This doesn't look like a DoseTrack backup file.")
        }
        val buffer = ByteBuffer.wrap(data, MAGIC.size, HEADER_BYTES - MAGIC.size)
        val version = buffer.get()
        if (version != VERSION) {
            throw InvalidImportFileException(
                "This backup was made by a newer version of DoseTrack. Update the app and try again.",
            )
        }
        val iterations = buffer.getInt()
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            throw InvalidImportFileException("The backup file is damaged.")
        }
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(data, 0, HEADER_BYTES)
        return try {
            cipher.doFinal(data, HEADER_BYTES, data.size - HEADER_BYTES)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(keyBytes, "AES").also { keyBytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }
}
