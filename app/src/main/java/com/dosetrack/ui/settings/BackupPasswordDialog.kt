package com.dosetrack.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dosetrack.R
import com.dosetrack.export.PasswordPolicy

/**
 * Asks for the backup password. With [isNewPassword] it asks twice and enforces [PasswordPolicy]
 * (export); otherwise it asks once (import). The password is handed over as a CharArray, which the
 * receiver clears after use.
 *
 * Uses the secure text fields (no copy/cut of the password, password keyboard, obscured text).
 * State is created with plain `remember`, deliberately not `rememberTextFieldState()`, which would
 * save the password into the saved instance state; the fields are cleared when the dialog closes.
 */
@Composable
fun BackupPasswordDialog(
    isNewPassword: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
) {
    val password = remember { TextFieldState() }
    val confirmation = remember { TextFieldState() }
    var visible by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            password.clearText()
            confirmation.clearText()
        }
    }

    val mismatch = isNewPassword && confirmation.text.isNotEmpty() && !confirmation.text.contentEquals(password.text)
    val canConfirm = if (isNewPassword) {
        PasswordPolicy.isAcceptable(password.text) && confirmation.text.contentEquals(password.text)
    } else {
        password.text.isNotEmpty()
    }

    val obfuscation = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped
    val toggle = @Composable {
        IconButton(onClick = { visible = !visible }) {
            Icon(
                painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                contentDescription = if (visible) "Hide password" else "Show password",
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewPassword) "Protect backup" else "Backup password") },
        text = {
            Column {
                Text(
                    if (isNewPassword) {
                        "The backup is encrypted with this password. You'll need it to import the backup. " +
                            "If you forget it, the backup can't be recovered."
                    } else {
                        "Enter the password that was set when this backup was exported."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedSecureTextField(
                    state = password,
                    label = { Text("Password") },
                    textObfuscationMode = obfuscation,
                    trailingIcon = toggle,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (isNewPassword) {
                    PasswordRuleChecklist(password.text)
                    OutlinedSecureTextField(
                        state = confirmation,
                        label = { Text("Repeat password") },
                        textObfuscationMode = obfuscation,
                        isError = mismatch,
                        supportingText = if (mismatch) ({ Text("Passwords don't match") }) else null,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Copy straight into a CharArray (no intermediate String) for the receiver to wipe.
                    val text = password.text
                    val chars = CharArray(text.length) { text[it] }
                    password.clearText()
                    confirmation.clearText()
                    onConfirm(chars)
                },
                enabled = canConfirm,
            ) {
                Text(if (isNewPassword) "Continue" else "Import")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Live checklist so the user sees which rule is still missing instead of a disabled button. */
@Composable
private fun PasswordRuleChecklist(password: CharSequence) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        PasswordPolicy.rules.forEach { rule ->
            val met = rule.isMet(password)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(if (met) R.drawable.ic_check_circle else R.drawable.ic_radio_button_unchecked),
                    contentDescription = if (met) "Met" else "Not met",
                    tint = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    rule.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
