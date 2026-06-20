package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FormatType {
    Wrap,
    PrependLine,
    InsertTemplate
}

data class FormatAction(
    val label: String,
    val description: String,
    val icon: ImageVector? = null,
    val prefix: String = "",
    val suffix: String = "",
    val type: FormatType = FormatType.Wrap,
    val template: String? = null
)

@Composable
fun FormatToolbar(
    onInsert: (FormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            HeadingDropdown(onInsert = onInsert)
            ToolbarDivider()
            ToolbarButton(
                FormatAction("B", "Bold", Icons.Default.FormatBold, "**", "**"),
                onInsert
            )
            ToolbarButton(
                FormatAction("I", "Italic", Icons.Default.FormatItalic, "*", "*"),
                onInsert
            )
            ToolbarButton(
                FormatAction("S", "Strikethrough", Icons.Default.FormatStrikethrough, "~~", "~~"),
                onInsert
            )
            ToolbarDivider()
            ToolbarButton(
                FormatAction("UL", "Unordered List", Icons.Default.FormatListBulleted, "- ", "", FormatType.PrependLine),
                onInsert
            )
            ToolbarButton(
                FormatAction("OL", "Ordered List", Icons.Default.FormatListNumbered, "1. ", "", FormatType.PrependLine),
                onInsert
            )
            ToolbarDivider()
            ToolbarButton(
                FormatAction("\u201C", "Blockquote", null, "> ", "", FormatType.PrependLine),
                onInsert
            )
            ToolbarButton(
                FormatAction("</>", "Inline Code", Icons.Default.Code, "`", "`"),
                onInsert
            )
            ToolbarButton(
                FormatAction("{}", "Code Block", null, "```\n", "\n```"),
                onInsert
            )
            ToolbarDivider()
            ToolbarButton(
                FormatAction("Link", "Link", Icons.Default.Link, "[", "](url)"),
                onInsert
            )
            ToolbarButton(
                FormatAction("Img", "Image", Icons.Default.Image, "![", "](url)"),
                onInsert
            )
            ToolbarButton(
                FormatAction("HR", "Horizontal Rule", null, "", "", FormatType.InsertTemplate, "\n---\n"),
                onInsert
            )
            ToolbarDivider()
            ToolbarButton(
                FormatAction("Σ", "Inline Math", null, "$", "$"),
                onInsert
            )
            ToolbarButton(
                FormatAction("ΣΣ", "Block Math", null, "$$", "$$"),
                onInsert
            )
            ToolbarButton(
                FormatAction("◇", "Mermaid", null, "", "", FormatType.InsertTemplate,
                    "\n```mermaid\ngraph TD\n    A[Start] --> B[Goal];\n```\n"),
                onInsert
            )
        }
    }
}

@Composable
private fun HeadingDropdown(onInsert: (FormatAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val headings = remember {
        listOf(
            FormatAction("H1", "Heading 1", null, "# ", "", FormatType.PrependLine),
            FormatAction("H2", "Heading 2", null, "## ", "", FormatType.PrependLine),
            FormatAction("H3", "Heading 3", null, "### ", "", FormatType.PrependLine),
            FormatAction("H4", "Heading 4", null, "#### ", "", FormatType.PrependLine),
            FormatAction("H5", "Heading 5", null, "##### ", "", FormatType.PrependLine),
            FormatAction("H6", "Heading 6", null, "###### ", "", FormatType.PrependLine),
        )
    }

    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = "H",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 0.dp)
        ) {
            headings.forEach { heading ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = heading.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onInsert(heading)
                        expanded = false
                    },
                    leadingIcon = {
                        Text(
                            text = "#".repeat(headings.size - headings.indexOf(heading)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    action: FormatAction,
    onInsert: (FormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onInsert(action) },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics {
            role = Role.Button
            contentDescription = action.description
            onClick { onInsert(action); true }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 4.dp)
                .defaultMinSize(minWidth = 26.dp, minHeight = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            if (action.icon != null) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.description,
                    modifier = Modifier.size(17.dp)
                )
            } else {
                Text(
                    text = action.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .padding(horizontal = 3.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}
