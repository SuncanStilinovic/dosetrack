package com.dosetrack.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dosetrack.export.PasswordPolicy

/**
 * Asks for the backup password. With [isNewPassword] it asks twice and enforces [PasswordPolicy]
 * (export); otherwise it asks once (import). The password is handed over as a CharArray, which the
 * receiver clears after use. State uses plain `remember`, never `rememberSaveable`, so the password
 * is not written into the saved instance state.
 */
@Composable
fun BackupPasswordDialog(
    isNewPassword: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val mismatch = isNewPassword && confirmation.isNotEmpty() && confirmation != password
    val canConfirm = if (isNewPassword) {
        PasswordPolicy.isAcceptable(password) && confirmation == password
    } else {
        password.isNotEmpty()
    }

    val transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    val toggle = @Composable {
        IconButton(onClick = { visible = !visible }) {
            Icon(
                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
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
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = transformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = toggle,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (isNewPassword) {
                    PasswordRuleChecklist(password)
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text("Repeat password") },
                        singleLine = true,
                        visualTransformation = transformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = mismatch,
                        supportingText = if (mismatch) ({ Text("Passwords don't match") }) else null,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.toCharArray()) }, enabled = canConfirm) {
                Text(if (isNewPassword) "Continue" else "Import")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Live checklist so the user sees which rule is still missing instead of a disabled button. */
@Composable
private fun PasswordRuleChecklist(password: String) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        PasswordPolicy.rules.forEach { rule ->
            val met = rule.isMet(password)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (met) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
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
