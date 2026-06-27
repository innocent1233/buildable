package com.libraryx.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Single source-of-truth dropdown composable for the entire app.
 *
 * Verified against Material3 1.3.1 (Compose BOM 2024.12.01):
 *
 *  - [ExposedDropdownMenuAnchorType] is the correct class name.
 *    `MenuAnchorType` does NOT exist in this package.
 *
 *  - [ExposedDropdownMenuBoxScope.menuAnchor] signature:
 *      abstract fun Modifier.menuAnchor(
 *          type: ExposedDropdownMenuAnchorType,
 *          enabled: Boolean = true
 *      ): Modifier
 *    The zero-arg overload is deprecated.
 *
 *  - [ExposedDropdownMenu] is an extension on [ExposedDropdownMenuBoxScope].
 *    It cannot be called via fully-qualified name — it must be called bare
 *    inside the [ExposedDropdownMenuBox] content lambda where the scope is implicit.
 *
 *  - [ExposedDropdownMenuBox], [ExposedDropdownMenuAnchorType], and [menuAnchor]
 *    all require @OptIn(ExperimentalMaterial3Api::class).
 *
 * Modifier split:
 *  - [modifier] is applied to the outer [ExposedDropdownMenuBox] (controls sizing / weight).
 *  - The inner [OutlinedTextField] always gets menuAnchor + fillMaxWidth so it fills the box.
 *    Applying the caller's modifier to the text field as well (via .then()) would double-apply
 *    sizing constraints and is incorrect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                // PrimaryNotEditable = anchor type for a non-editable/readOnly text field.
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        // Called bare (not as androidx.compose.material3.ExposedDropdownMenu) because
        // it is an extension on ExposedDropdownMenuBoxScope. The scope is available
        // here implicitly; using a fully-qualified name would lose the receiver and
        // produce "Unresolved reference" at compile time.
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
