package com.dosetrack.ui.guide

import com.dosetrack.data.settings.AppSettings
import com.dosetrack.export.PasswordPolicy

/** One piece of a guide section. */
sealed interface GuideBlock {
    data class Paragraph(val text: String) : GuideBlock

    /** A numbered sequence of actions. */
    data class Steps(val items: List<String>) : GuideBlock

    data class Bullets(val items: List<String>) : GuideBlock

    /** Highlighted advice or a warning. */
    data class Tip(val text: String) : GuideBlock
}

data class GuideSection(val title: String, val blocks: List<GuideBlock>)

/**
 * The in-app user manual. Labels in quotes match the app's buttons and fields exactly, so keep this
 * in step with the UI when either changes.
 */
object GuideContent {

    const val INTRO = "DoseTrack is a private diary for your medications: when you took each dose, " +
        "where you injected it, and how your body reacted. Everything stays on this phone."

    val sections: List<GuideSection> = listOf(
        GuideSection(
            "Getting started",
            listOf(
                GuideBlock.Paragraph(
                    "DoseTrack works in four simple steps. You can come back to this guide at any time " +
                        "from the Guide tab.",
                ),
                GuideBlock.Steps(
                    listOf(
                        "Open the Medications tab and tap + to add your medication.",
                        "On the same screen, set a reminder if you want one.",
                        "When you take a dose, go to Home and tap \"Log Drug\".",
                        "Open the History tab to see, edit or delete what you've logged.",
                    ),
                ),
                GuideBlock.Tip("Add your medications first: logging is quicker when you just pick one from a list."),
            ),
        ),
        GuideSection(
            "Medications",
            listOf(
                GuideBlock.Paragraph("Open the Medications tab. Tap + to add a medication, or tap one in the list to edit it."),
                GuideBlock.Bullets(
                    listOf(
                        "Name: what you call it, for example the brand name on the box.",
                        "How it's taken: Injection, Oral, Topical, Inhaled or Other. Only injections are logged " +
                            "on the body diagram; for the others you just record the dose.",
                        "Dosage amount and Unit: the dose you take each time, in mg, mL, mcg, IU or units.",
                    ),
                ),
                GuideBlock.Paragraph(
                    "Each row in the list shows the dose, how it's taken and, if set, its reminder. To delete a " +
                        "medication, tap the bin icon. Its reminder is turned off, and entries you logged with it " +
                        "keep showing its name in History.",
                ),
                GuideBlock.Tip(
                    "If your dose changes, add it as a new medication (for example \"0.5 mg\" and \"1 mg\"). " +
                        "Your history then shows exactly which dose you took each time.",
                ),
            ),
        ),
        GuideSection(
            "Reminders",
            listOf(
                GuideBlock.Paragraph(
                    "Each medication can have its own reminder. Open the medication from the Medications tab and " +
                        "scroll down to \"Reminder\". Choose one of:",
                ),
                GuideBlock.Bullets(
                    listOf(
                        "Off: no reminder.",
                        "Specific days of week: pick one or more days and tap the time button to choose the time.",
                        "Every X hours/days/weeks/months: enter how often, choose the unit, and set the date and " +
                            "time of the first reminder. The rest follow from there.",
                    ),
                ),
                GuideBlock.Paragraph("Under \"Ends\", choose when the reminder stops:"),
                GuideBlock.Bullets(
                    listOf(
                        "Never: it keeps reminding you until you turn it off.",
                        "On a date: the last reminder is on that day; nothing comes after it.",
                        "After a number of doses: it stops once you've logged that many doses of this medication. " +
                            "Only doses logged after you set the reminder count. The screen shows how many you've " +
                            "logged so far.",
                    ),
                ),
                GuideBlock.Paragraph(
                    "Tap \"Save\". When a reminder has ended, the medication list shows \"Reminder ended\". To start " +
                        "it again, change when it ends; changing the end rule also restarts the dose count.",
                ),
                GuideBlock.Paragraph(
                    "When a reminder goes off, tap the notification: the log screen opens with that medication " +
                        "already selected.",
                ),
                GuideBlock.Tip(
                    "Reminders need two permissions. If the reminder section shows \"Allow notifications\" or " +
                        "\"Allow exact alarms\", tap them so reminders arrive on time. Reminders keep working after " +
                        "your phone restarts or the time zone changes.",
                ),
                GuideBlock.Paragraph(
                    "With a screen lock set, the lock screen only says \"You have a reminder\". The medication name " +
                        "is shown once the phone is unlocked.",
                ),
            ),
        ),
        GuideSection(
            "Logging an injection or dose",
            listOf(
                GuideBlock.Steps(
                    listOf(
                        "Go to Home and tap \"Log Drug\" (or tap a reminder notification).",
                        "Choose the medication at the top.",
                        "For an injection, tap the square on the body diagram where you injected.",
                        "Check the date and time. They start at now; tap the field to change them if you are " +
                            "logging a dose you took earlier.",
                        "If you had a reaction, turn on \"Had a reaction?\" and describe it.",
                        "Add any notes, then tap \"Save\".",
                    ),
                ),
                GuideBlock.Paragraph(
                    "Injection sites are grouped into zones: beside the navel, on the upper arms and on the front " +
                        "of the thighs. You can only choose a square inside a zone.",
                ),
                GuideBlock.Tip(
                    "R and L mean your right and left. The figure faces you, like a person standing in front " +
                        "of you, so your right side is on the left of the screen.",
                ),
                GuideBlock.Paragraph(
                    "Shaded squares are the sites you used most recently, and small dots mark the middle of each " +
                        "zone. You choose how many recent sites are shaded in Settings.",
                ),
                GuideBlock.Paragraph(
                    "If the medication isn't injected (for example a tablet), the body diagram disappears. " +
                        "Just check the time and save.",
                ),
                GuideBlock.Paragraph(
                    "\"Save\" stays greyed out until the entry is complete: an injection needs a site, and the date " +
                        "can't be in the future.",
                ),
            ),
        ),
        GuideSection(
            "Rotating injection sites",
            listOf(
                GuideBlock.Paragraph(
                    "Injecting into the same spot again and again can irritate the skin. The shaded squares show " +
                        "where you injected recently, so you can easily pick a different square, or a different " +
                        "zone, next time.",
                ),
                GuideBlock.Tip(
                    "DoseTrack is a memory aid, not medical advice. Always follow your doctor's or pharmacist's " +
                        "instructions on where and how to inject.",
                ),
            ),
        ),
        GuideSection(
            "Notes",
            listOf(
                GuideBlock.Paragraph(
                    "To write something down without logging a dose, go to Home and tap \"Add Note\". Use it for " +
                        "symptoms, side effects, a missed dose, or anything you want to tell your doctor. Notes " +
                        "appear in History alongside your doses.",
                ),
            ),
        ),
        GuideSection(
            "History",
            listOf(
                GuideBlock.Paragraph("The History tab lists everything you've logged, newest first."),
                GuideBlock.Bullets(
                    listOf(
                        "The small body figure shows where you injected.",
                        "A note icon marks a note; a medication icon marks a dose of a medication that isn't injected.",
                        "A ⚠ warning sign marks an entry where you had a reaction.",
                    ),
                ),
                GuideBlock.Paragraph(
                    "Tap an entry to edit it. Tap the bin icon to delete it; you'll be asked to confirm, and a " +
                        "deleted entry can't be brought back.",
                ),
                GuideBlock.Tip(
                    "Deleting a dose also updates reminders that end after a number of doses, so a reminder that " +
                        "had ended may start again.",
                ),
            ),
        ),
        GuideSection(
            "Settings",
            listOf(
                GuideBlock.Bullets(
                    listOf(
                        "Previous sites to show: how many recent injection sites are shaded on the body diagram " +
                            "(${AppSettings.SITE_HISTORY_RANGE.first}–${AppSettings.SITE_HISTORY_RANGE.last}).",
                        "Clock: show and pick times on a 12-hour (1:30 PM) or 24-hour (13:30) clock.",
                        "Data: back up your data or restore it. See \"Backups and moving to a new phone\".",
                    ),
                ),
            ),
        ),
        GuideSection(
            "Backups and moving to a new phone",
            listOf(
                GuideBlock.Paragraph(
                    "A backup is one file with all your medications, reminders, history and settings, locked with a " +
                        "password you choose.",
                ),
                GuideBlock.Paragraph("To make a backup, open Settings and tap \"Export backup\" or \"Share\":"),
                GuideBlock.Steps(
                    listOf(
                        "Choose a password and type it twice.",
                        "With \"Export backup\", choose where to save the file, for example your Downloads folder. " +
                            "With \"Share\", choose an app to send it with.",
                    ),
                ),
                GuideBlock.Paragraph("The password must have:"),
                GuideBlock.Bullets(PasswordPolicy.rules.map { it.description }),
                GuideBlock.Tip(
                    "Nobody can open a backup without its password, not even the developer. If you forget it, the " +
                        "backup can't be recovered. Keep the password somewhere safe, separately from the file.",
                ),
                GuideBlock.Paragraph("To restore a backup:"),
                GuideBlock.Steps(
                    listOf(
                        "Open Settings and tap \"Import backup\".",
                        "Choose the backup file.",
                        "Enter its password.",
                    ),
                ),
                GuideBlock.Paragraph(
                    "Entries that are already in the app are skipped, so importing the same file twice is " +
                        "harmless. Settings, and the reminders of the medications in the backup, are replaced by " +
                        "the ones in the file.",
                ),
                GuideBlock.Paragraph(
                    "Moving to a new phone: export a backup on the old phone, copy the file to the new phone, " +
                        "install DoseTrack there and import it.",
                ),
            ),
        ),
        GuideSection(
            "Privacy and security",
            listOf(
                GuideBlock.Bullets(
                    listOf(
                        "DoseTrack has no internet access, no account, no ads and no analytics. It can't send your " +
                            "data anywhere.",
                        "Your data is stored only on this phone, and it isn't included in Google's automatic " +
                            "cloud backup. Only a backup you make yourself, or a direct phone-to-phone transfer " +
                            "when you set up a new phone, copies it.",
                        "Use a screen lock (PIN, pattern or fingerprint): it is the main protection for the health " +
                            "information you keep here.",
                        "Uninstalling DoseTrack deletes all of its data from the phone.",
                    ),
                ),
                GuideBlock.Paragraph("The full privacy policy is linked from DoseTrack's page on Google Play."),
            ),
        ),
        GuideSection(
            "Troubleshooting",
            listOf(
                GuideBlock.Paragraph("A reminder didn't arrive"),
                GuideBlock.Bullets(
                    listOf(
                        "Open the medication and check that the reminder is on and hasn't ended.",
                        "If the reminder section shows \"Allow notifications\" or \"Allow exact alarms\", tap them.",
                        "Some phones stop apps in the background to save battery. In your phone's settings, set " +
                            "DoseTrack's battery use to \"Unrestricted\" or \"Don't optimise\".",
                    ),
                ),
                GuideBlock.Paragraph("\"Save\" is greyed out"),
                GuideBlock.Bullets(
                    listOf(
                        "When logging: choose a site on the body diagram and check the date isn't in the future.",
                        "When adding a medication: enter a name and a dose greater than 0. If a reminder is on, " +
                            "pick at least one day and complete the \"Ends\" choice.",
                    ),
                ),
                GuideBlock.Paragraph("Importing says the backup was made by a newer version"),
                GuideBlock.Bullets(listOf("Update DoseTrack from Google Play, then import again.")),
                GuideBlock.Paragraph("Times show in the wrong format"),
                GuideBlock.Bullets(listOf("Choose 12-hour or 24-hour under \"Clock\" in Settings.")),
            ),
        ),
        GuideSection(
            "About DoseTrack",
            listOf(
                GuideBlock.Paragraph(
                    "DoseTrack is free and open source (GPL-3.0), with no ads and no data collection.",
                ),
                GuideBlock.Tip(
                    "DoseTrack is a personal logging tool, not a medical device. It doesn't give medical advice or " +
                        "recommend doses. Always follow the instructions of your doctor or pharmacist.",
                ),
            ),
        ),
    )
}
