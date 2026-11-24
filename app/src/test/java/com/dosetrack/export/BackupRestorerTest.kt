package com.dosetrack.export

import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.dao.MedicationDao
import com.dosetrack.data.db.dao.MedicationReminderDao
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.InjectionWithMedicationEntity
import com.dosetrack.data.db.entities.MedicationEntity
import com.dosetrack.data.db.entities.MedicationReminderEntity
import com.dosetrack.data.db.entities.SiteCoordinates
import com.dosetrack.data.db.entities.toDomain
import com.dosetrack.data.db.entities.toEntity
import com.dosetrack.data.model.MedicationReminder
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.model.ReminderMode
import java.time.DayOfWeek
import com.dosetrack.data.model.AdministrationRoute
import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.SitePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BackupRestorerTest {

    private val now = Instant.parse("2026-11-21T12:00:00Z")
    private val t0 = Instant.parse("2026-11-20T09:54:37Z")

    private val medications = FakeMedicationDao()
    private val injections = FakeInjectionDao()
    private val reminders = FakeReminderDao()
    private val restorer = BackupRestorer(medications, injections, reminders) { now }

    private fun med(id: Long, name: String = "Ozempic", amount: Double = 0.5, unit: DosageUnit = DosageUnit.ML, deleted: Boolean = false) =
        BackupMedication(id, name, amount, unit, deleted, t0)

    private fun shot(
        medicationId: Long? = 1,
        at: Instant = t0,
        site: SitePoint? = SitePoint(0.4f, 0.4f),
        notes: String? = null,
        createdAt: Instant? = t0,
    ) = BackupInjection(at, site == null, site, medicationId, false, null, notes, createdAt, createdAt)

    private fun restore(meds: List<BackupMedication>, shots: List<BackupInjection>) =
        runBlocking { restorer.restore(ParsedBackup(meds, shots, settings = null, invalidRows = 0)) }

    @Test
    fun `restores into an empty database`() {
        val result = restore(listOf(med(1), med(2, "Unused", 10.0, DosageUnit.MG)), listOf(shot(), shot(at = t0.minusSeconds(86_400))))

        assertEquals(2, result.medicationsCreated)
        assertEquals(2, result.imported)
        assertEquals(0, result.duplicates)
        assertEquals(setOf("Ozempic", "Unused"), medications.rows.map { it.name }.toSet())
        val ozempicId = medications.rows.single { it.name == "Ozempic" }.id
        injections.rows.forEach { assertEquals(ozempicId, it.medicationId) }
    }

    @Test
    fun `medication route is restored, with site-less doses`() {
        val tablet = med(1, "Tablet", 10.0, DosageUnit.MG).copy(route = AdministrationRoute.ORAL)
        val dose = shot(site = null).copy(isNoteOnly = false)
        val result = restore(listOf(tablet), listOf(dose))

        assertEquals(1, result.imported)
        assertEquals(AdministrationRoute.ORAL.name, medications.rows.single().route)
        val row = injections.rows.single()
        assertFalse(row.isNoteOnly)
        assertNull(row.siteX)
    }

    @Test
    fun `backup ids are remapped to new local ids`() {
        medications.rows += MedicationEntity(id = 1, name = "Local", dosageAmount = 1.0, dosageUnit = "MG", createdAt = 0)
        medications.nextId = 2
        restore(listOf(med(1, "FromBackup")), listOf(shot(medicationId = 1)))

        val fromBackup = medications.rows.single { it.name == "FromBackup" }
        assertEquals(fromBackup.id, injections.rows.single().medicationId)
    }

    @Test
    fun `existing medication with same name and dosage is reused, ignoring case`() {
        medications.rows += MedicationEntity(id = 5, name = "OZEMPIC", dosageAmount = 0.5, dosageUnit = "ML", createdAt = 0)
        medications.nextId = 6
        val result = restore(listOf(med(1)), listOf(shot()))

        assertEquals(0, result.medicationsCreated)
        assertEquals(1, medications.rows.size)
        assertEquals(5L, injections.rows.single().medicationId)
    }

    @Test
    fun `same name with a different dosage is a different medication`() {
        medications.rows += MedicationEntity(id = 5, name = "Ozempic", dosageAmount = 1.0, dosageUnit = "ML", createdAt = 0)
        medications.nextId = 6
        val result = restore(listOf(med(1)), emptyList())
        assertEquals(1, result.medicationsCreated)
        assertEquals(2, medications.rows.size)
    }

    @Test
    fun `medication deleted locally but active in the backup is brought back`() {
        medications.rows += MedicationEntity(id = 5, name = "Ozempic", dosageAmount = 0.5, dosageUnit = "ML", isDeleted = true, createdAt = 0)
        medications.nextId = 6
        restore(listOf(med(1)), emptyList())
        assertFalse(medications.rows.single().isDeleted)
    }

    @Test
    fun `medication deleted in the backup stays deleted`() {
        restore(listOf(med(1, deleted = true)), emptyList())
        assertEquals(true, medications.rows.single().isDeleted)
    }

    @Test
    fun `restoring the same backup twice adds nothing the second time`() {
        val backup = listOf(shot(), shot(at = t0.minusSeconds(3600), notes = "n"), shot(medicationId = null, site = null, notes = "note"))
        restore(listOf(med(1)), backup)
        val second = restore(listOf(med(1)), backup)

        assertEquals(0, second.imported)
        assertEquals(3, second.duplicates)
        assertEquals(0, second.medicationsCreated)
        assertEquals(3, injections.rows.size)
        assertEquals(1, medications.rows.size)
    }

    @Test
    fun `old minute-precision export matches an existing entry with seconds`() {
        restore(listOf(med(1)), listOf(shot(at = Instant.parse("2026-11-20T09:54:37Z"))))
        val legacy = restore(listOf(med(1)), listOf(shot(at = Instant.parse("2026-11-20T09:54:00Z"), createdAt = null)))
        assertEquals(1, legacy.duplicates)
    }

    @Test
    fun `same minute but different site or notes is not a duplicate`() {
        restore(listOf(med(1)), listOf(shot()))
        val result = restore(listOf(med(1)), listOf(shot(site = SitePoint(0.6f, 0.4f)), shot(notes = "different")))
        assertEquals(2, result.imported)
    }

    @Test
    fun `missing created and updated times fall back to now`() {
        restore(listOf(med(1)), listOf(shot(createdAt = null)))
        val row = injections.rows.single()
        assertEquals(now.toEpochMilli(), row.createdAt)
        assertEquals(now.toEpochMilli(), row.updatedAt)
    }

    @Test
    fun `reaction note is dropped when there was no reaction`() {
        val inconsistent = shot().copy(hadReaction = false, reactionNote = "stale")
        restore(listOf(med(1)), listOf(inconsistent))
        assertNull(injections.rows.single().reactionNote)
    }

    @Test
    fun `reminders attach to the remapped medication and replace an existing one`() {
        medications.rows += MedicationEntity(id = 1, name = "Local", dosageAmount = 1.0, dosageUnit = "MG", createdAt = 0)
        medications.nextId = 2
        reminders.rows[1] = MedicationReminder(1, ReminderConfig(mode = ReminderMode.INTERVAL)).toEntity()
        val weekly = ReminderConfig(mode = ReminderMode.DAYS_OF_WEEK, days = setOf(DayOfWeek.MONDAY), startAt = t0, countFrom = t0)
        val result = runBlocking {
            restorer.restore(
                ParsedBackup(
                    medications = listOf(med(1, "FromBackup"), med(5, "Local", 1.0, DosageUnit.MG)),
                    injections = emptyList(),
                    settings = null,
                    invalidRows = 0,
                    reminders = listOf(BackupReminder(1, weekly), BackupReminder(5, weekly)),
                ),
            )
        }

        assertEquals(2, result.remindersRestored)
        val fromBackupId = medications.rows.single { it.name == "FromBackup" }.id
        assertEquals(setOf(1L, fromBackupId), reminders.rows.keys)
        // The local "Local" medication matched backup id 5, so its reminder was replaced.
        assertEquals(ReminderMode.DAYS_OF_WEEK, reminders.rows.getValue(1).toDomain().config.mode)
    }

    @Test
    fun `invalid row count from parsing is reported`() {
        val result = runBlocking { restorer.restore(ParsedBackup(emptyList(), emptyList(), null, invalidRows = 4)) }
        assertEquals(4, result.invalid)
    }
}

private class FakeMedicationDao : MedicationDao {
    val rows = mutableListOf<MedicationEntity>()
    var nextId = 1L

    override suspend fun insert(medication: MedicationEntity): Long {
        val id = nextId++
        rows += medication.copy(id = id)
        return id
    }

    override suspend fun update(medication: MedicationEntity) {
        rows[rows.indexOfFirst { it.id == medication.id }] = medication
    }

    override suspend fun softDelete(id: Long) {
        rows[rows.indexOfFirst { it.id == id }] = rows.first { it.id == id }.copy(isDeleted = true)
    }

    override fun observeActive(): Flow<List<MedicationEntity>> = flowOf(rows.filter { !it.isDeleted })
    override fun observeAll(): Flow<List<MedicationEntity>> = flowOf(rows.toList())
    override suspend fun getAll(): List<MedicationEntity> = rows.toList()
    override suspend fun getById(id: Long): MedicationEntity? = rows.firstOrNull { it.id == id }
}

private class FakeInjectionDao : InjectionDao {
    val rows = mutableListOf<InjectionEntity>()
    private var nextId = 1L

    override suspend fun insert(injection: InjectionEntity): Long {
        val id = nextId++
        rows += injection.copy(id = id)
        return id
    }

    override suspend fun update(injection: InjectionEntity) {
        rows[rows.indexOfFirst { it.id == injection.id }] = injection
    }

    override suspend fun delete(injection: InjectionEntity) {
        rows.removeAll { it.id == injection.id }
    }

    override suspend fun getById(id: Long): InjectionEntity? = rows.firstOrNull { it.id == id }

    override suspend fun getInRange(fromMillis: Long, toMillis: Long): List<InjectionEntity> =
        rows.filter { it.timestamp in fromMillis until toMillis }

    override fun observeAllWithMedication(): Flow<List<InjectionWithMedicationEntity>> =
        throw UnsupportedOperationException()

    override suspend fun getRecentSites(limit: Int, excludeId: Long): List<SiteCoordinates> =
        throw UnsupportedOperationException()

    override suspend fun countDoses(medicationId: Long, fromMillis: Long): Int =
        rows.count { it.medicationId == medicationId && !it.isNoteOnly && it.timestamp >= fromMillis }
}

private class FakeReminderDao : MedicationReminderDao {
    val rows = mutableMapOf<Long, MedicationReminderEntity>()

    override suspend fun upsert(reminder: MedicationReminderEntity) {
        rows[reminder.medicationId] = reminder
    }

    override suspend fun delete(medicationId: Long) {
        rows.remove(medicationId)
    }

    override suspend fun get(medicationId: Long): MedicationReminderEntity? = rows[medicationId]
    override suspend fun getActive(): List<MedicationReminderEntity> = rows.values.toList()
    override fun observeActive(): Flow<List<MedicationReminderEntity>> = flowOf(rows.values.toList())
    override suspend fun getAll(): List<MedicationReminderEntity> = rows.values.toList()
}
