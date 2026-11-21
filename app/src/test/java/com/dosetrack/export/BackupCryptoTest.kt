package com.dosetrack.export

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class BackupCryptoTest {

    private val plaintext = "Date,Time\nšđčćž 💉,x\n".toByteArray(Charsets.UTF_8)
    private val password = "Correct-Horse7"

    /** The minimum the decrypter accepts; keeps the suite fast. The default count is tested once below. */
    private val fastIterations = 100_000

    private fun encrypt(pw: String = password, data: ByteArray = plaintext) =
        BackupCrypto.encrypt(data, pw.toCharArray(), iterations = fastIterations)

    @Test
    fun `round trip with the default iteration count`() {
        val encrypted = BackupCrypto.encrypt(plaintext, password.toCharArray())
        assertArrayEquals(plaintext, BackupCrypto.decrypt(encrypted, password.toCharArray()))
    }

    @Test
    fun `encrypted output is recognised, plain CSV is not`() {
        assertTrue(BackupCrypto.isEncrypted(encrypt()))
        assertFalse(BackupCrypto.isEncrypted(plaintext))
        assertFalse(BackupCrypto.isEncrypted(ByteArray(0)))
    }

    @Test
    fun `plaintext does not appear in the output`() {
        val marker = "SECRET-MARKER-123".toByteArray()
        val encrypted = encrypt(data = marker)
        assertFalse(encrypted.toList().windowed(marker.size).any { it == marker.toList() })
    }

    @Test
    fun `wrong password is reported as such`() {
        val encrypted = encrypt()
        assertThrows(WrongPasswordException::class.java) {
            BackupCrypto.decrypt(encrypted, "Correct-Horse8".toCharArray())
        }
    }

    @Test
    fun `modified ciphertext is rejected`() {
        val encrypted = encrypt()
        encrypted[encrypted.size - 20] = (encrypted[encrypted.size - 20].toInt() xor 1).toByte()
        assertThrows(WrongPasswordException::class.java) { BackupCrypto.decrypt(encrypted, password.toCharArray()) }
    }

    @Test
    fun `modified header is rejected because it is authenticated`() {
        val encrypted = encrypt()
        ByteBuffer.wrap(encrypted).putInt(5, fastIterations + 1) // iteration count field
        assertThrows(WrongPasswordException::class.java) { BackupCrypto.decrypt(encrypted, password.toCharArray()) }
    }

    @Test
    fun `iteration counts outside the safe range are refused before key derivation`() {
        val tooLow = encrypt().also { ByteBuffer.wrap(it).putInt(5, 1) }
        val tooHigh = encrypt().also { ByteBuffer.wrap(it).putInt(5, Int.MAX_VALUE) }
        assertThrows(InvalidImportFileException::class.java) { BackupCrypto.decrypt(tooLow, password.toCharArray()) }
        assertThrows(InvalidImportFileException::class.java) { BackupCrypto.decrypt(tooHigh, password.toCharArray()) }
    }

    @Test
    fun `unknown format version is refused`() {
        val encrypted = encrypt().also { it[4] = 99 }
        assertThrows(InvalidImportFileException::class.java) { BackupCrypto.decrypt(encrypted, password.toCharArray()) }
    }

    @Test
    fun `truncated file is refused`() {
        val truncated = encrypt().copyOf(20)
        assertThrows(InvalidImportFileException::class.java) { BackupCrypto.decrypt(truncated, password.toCharArray()) }
    }

    @Test
    fun `same input encrypts differently each time (random salt and IV)`() {
        assertFalse(encrypt().contentEquals(encrypt()))
    }
}
