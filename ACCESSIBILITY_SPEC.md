# Markdown Vault - Accessibility Implementation Specification

## Overview
This document outlines the comprehensive accessibility improvements for the Markdown Vault Android application to meet WCAG 2.1 AA standards and Android accessibility best practices.

---

## 1. High-Contrast Theme System

### 1.1 New Theme Variants
- **HIGH_CONTRAST_DARK**: WCAG AAA compliant (7:1 contrast ratio)
- **HIGH_CONTRAST_LIGHT**: WCAG AAA compliant (7:1 contrast ratio)

### 1.2 Color Specifications

#### High Contrast Dark
| Token | Value | Contrast Ratio (vs background) |
|-------|-------|-------------------------------|
| background | #000000 | - |
| surface | #1A1A1A | 1:1 (base) |
| surfaceVariant | #2D2D2D | 1.6:1 |
| primary | #FFFFFF | 21:1 |
| onPrimary | #000000 | 21:1 |
| secondary | #00FFFF | 8.6:1 |
| onSecondary | #000000 | 8.6:1 |
| outline | #FFFFFF | 21:1 |
| error | #FF5252 | 5.4:1 |
| onError | #FFFFFF | 5.4:1 |

#### High Contrast Light
| Token | Value | Contrast Ratio (vs background) |
|-------|-------|-------------------------------|
| background | #FFFFFF | - |
| surface | #F5F5F5 | 1:1 |
| surfaceVariant | #E0E0E0 | 1.6:1 |
| primary | #000000 | 21:1 |
| onPrimary | #FFFFFF | 21:1 |
| secondary | #0066CC | 7.2:1 |
| onSecondary | #FFFFFF | 7.2:1 |
| outline | #000000 | 21:1 |
| error | #D32F2F | 5.4:1 |
| onError | #FFFFFF | 5.4:1 |

### 1.3 Implementation Files
- `Color.kt`: Add high-contrast color constants
- `Theme.kt`: Add `HIGH_CONTRAST_DARK` and `HIGH_CONTRAST_LIGHT` theme variants
- `SettingsScreen`: Add theme selection options

---

## 2. Semantic Accessibility Labels & Content Descriptions

### 2.1 Required Updates by Component

#### DashboardScreen.kt
| Element | Current State | Required Accessibility |
|---------|---------------|------------------------|
| Navigation drawer toggle | `contentDescription = "Open Sidebar"` | Add `semantics { role = Role.Button }` |
| Sync button | `contentDescription = "Sync Vault"` | Add state description: "Sync in progress" / "Sync available" |
| Delete note button | `contentDescription = "Delete Note"` | Add confirmation announcement |
| Vault list items | Surface `onClick` without role | Add `role = Role.MenuItem`, `selected` state |
| Settings button | `contentDescription = "Settings"` | Add `role = Role.Button` |

#### SidebarContent
| Element | Required |
|---------|----------|
| Vault items | `role = Role.MenuItem`, `selected` state |
| "Connect New Vault" | `role = Role.MenuItem` |
| Settings entry | `role = Role.MenuItem` |

#### NoteWorkspace
| Element | Required |
|---------|----------|
| Mode selector (Preview/Edit) | `role = Role.Tab`, `selected` state |
| Editor toolbar buttons | `role = Role.Button`, descriptive labels |
| Math/Mermaid panel | `role = Role.Region`, `heading` for section title |

#### MarkdownEditorArea
| Element | Required |
|---------|----------|
| Formatting toolbar buttons | `contentDescription` for each: "Bold", "Italic", "Header", "Inline Math", "Block Math", "Mermaid" |
| Template chips | `role = Role.Button`, `contentDescription = "Insert $title template"` |
| Text field | `role = Role.TextField`, `label = "Markdown Editor"` |

#### SettingsScreen
| Element | Required |
|---------|----------|
| Theme selection chips | `role = Role.RadioButton`, `selected` state |
| Text fields | `label` parameter |
| Switch | Built-in accessibility (verify) |
| Buttons | `contentDescription` where icon-only |

#### Dialogs (AddVault, AddNote, AddFolder, ConflictResolver)
| Element | Required |
|---------|----------|
| All text fields | `label` parameter |
| All buttons | `contentDescription` or visible text |
| Dropdown menus | `contentDescription` on trigger |

#### FolderExplorer
| Element | Required |
|---------|----------|
| Search field | `label = "Search files and folders"` |
| Breadcrumb links | `role = Role.Link`, `contentDescription = "Navigate to $segment"` |
| Folder cards | `role = Role.Button`, `contentDescription = "Open folder $name"` |
| Folder menu items | `role = Role.MenuItem` |

---

## 3. Minimum Touch Targets (48x48dp)

### 3.1 Current Violations (Identified)
| Component | Current Size | Required Fix |
|-----------|--------------|--------------|
| IconButton (32dp) | 32x32dp | Wrap in 48x48dp touch target |
| FilterChip (compact) | ~32dp height | Increase minHeight to 48dp |
| Toolbar action icons | 24dp icon only | Ensure 48x48dp touch area |
| FAB menu items | 44dp FAB | OK, but labels need touch area |
| Tab switcher boxes | ~36dp | Increase to 48dp |
| Dropdown menu items | ~40dp | Increase minHeight |

### 3.2 Fix Strategy
```kotlin
// Standard touch target modifier
val MinimumTouchTarget = Modifier
    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    .wrapContentSize(Alignment.Center)

// For IconButtons
IconButton(onClick = { ... }, modifier = Modifier.size(48.dp)) { ... }
```

---

## 4. Skip to Main Content Navigation

### 4.1 Implementation
- Add invisible "Skip to main content" button at top of composition hierarchy
- Visible only when focused (TalkBack/Keyboard navigation)
- Navigates to main content area (Scaffold innerPadding Box)

### 4.2 Code Pattern
```kotlin
@Composable
fun SkipToMainContent(targetModifier: Modifier = Modifier) {
    var focusRequested by remember { mutableStateOf(false) }
    Text(
        text = "Skip to main content",
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
            .focusTarget()
            .focusProperties { focusProperties ->
                focusProperties.canFocus = true
            }
            .onFocusChanged { if (it.isFocused) focusRequested = true }
            .alpha(if (focusRequested) 1f else 0f)
            .animateContentSize()
            .semantics { role = Role.Link }
    )
}
```

---

## 5. MarkdownPreview WebView Accessibility

### 5.1 HTML Template Enhancements
```html
<!-- Add to head -->
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">

<!-- Semantic structure -->
<article id="content" role="article">
  <!-- headings get automatic heading navigation -->
  <h1>...</h1>
  <h2>...</h2>
</article>

<!-- Images must have alt text -->
<img src="..." alt="User-provided or generated description">

<!-- Code blocks -->
<pre><code class="language-kotlin">...</code></pre>

<!-- Tables -->
<table>
  <thead><tr><th scope="col">...</th></tr></thead>
  <tbody>...</tbody>
</table>
```

### 5.2 WebView Accessibility Configuration
```kotlin
webView.apply {
    settings.javaScriptEnabled = true
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.textZoom = 100
    // Enable accessibility
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    // Allow TalkBack to traverse web content
    setAccessibilityDelegate(WebViewAccessibilityDelegate())
}
```

### 5.3 Marked.js Configuration for Accessibility
```javascript
marked.setOptions({
    gfm: true,
    breaks: true,
    // Ensure heading IDs for navigation
    headerIds: true,
    // Add ARIA labels to elements
    mangle: false
});

// Post-process for images without alt
document.querySelectorAll('img:not([alt])').forEach(img => {
    img.alt = 'Image: ' + (img.title || img.src.split('/').pop() || 'No description');
});
```

---

## 6. Focus Management & Screen Reader Announcements

### 6.1 Focus Order
1. Skip to main content link
2. Top app bar (navigation, title, actions)
3. Sidebar (if tablet/desktop)
4. Main content area
5. Floating action button
6. Bottom navigation (if any)

### 6.2 Screen Reader Announcements
| Event | Announcement |
|-------|--------------|
| Sync started | "Sync started, synchronizing vault with GitHub" |
| Sync completed | "Sync completed successfully" / "Sync failed: {error}" |
| Note saved | "Note saved" |
| Note deleted | "Note deleted" |
| Conflict detected | "Merge conflict detected for {note title}" |
| Theme changed | "Theme changed to {theme name}" |
| Vault switched | "Switched to vault {vault name}" |

### 6.3 Implementation
```kotlin
val accessibilityManager = LocalAccessibilityManager.current
accessibilityManager.announceForAccessibility("Sync completed successfully")
```

---

## 7. Typography & Scaling Support

### 7.1 Font Size Scaling
- Ensure all text uses `sp` units (already done)
- Test with "Large" and "Largest" font sizes in Accessibility settings
- Prevent text clipping with `minLines`/`maxLines` and proper constraints

### 7.2 Line Height & Spacing
- Minimum line height: 1.5x font size
- Paragraph spacing: 1.5x line height
- Already implemented in MarkdownPreview CSS (line-height: 1.6)

---

## 8. Color Independence

### 8.1 Requirements
- No information conveyed by color alone
- Error states: icon + text + color
- Active states: indicator + color + text label
- Focus indicators: visible outline (3dp minimum)

### 8.2 Current Issues to Fix
- Active vault indicator: Only color (purple border) → Add check icon + "Active" label
- Sync status: Only color (green/red) → Add icon + text status
- Error fields: Only red border → Add error icon + helper text

---

## 9. Motion Reduction

### 9.1 Respect `prefers-reduced-motion`
- Disable `AnimatedContent` transitions when enabled
- Disable `AnimatedVisibility` animations
- Keep instant state changes

### 9.2 Implementation
```kotlin
val reduceMotion = LocalConfiguration.current.fontScale > 1.3f // Proxy
// Or use WindowInsetsAnimationController for API 30+
```

---

## 10. Testing Checklist

### 10.1 Automated
- [ ] Accessibility Scanner (Play Store) - Zero errors
- [ ] Layout Inspector - Verify touch targets ≥ 48dp
- [ ] Compose Preview with `uiMode = Configuration.UI_MODE_NIGHT_YES`

### 10.2 Manual (TalkBack)
- [ ] Navigate entire app with TalkBack enabled
- [ ] Verify all images have descriptions
- [ ] Verify heading navigation in MarkdownPreview
- [ ] Verify form field labels announced
- [ ] Verify error messages announced
- [ ] Verify "Skip to main content" works

### 10.3 Manual (Switch Access)
- [ ] Navigate with linear scanning
- [ ] Verify all interactive elements reachable
- [ ] Verify no keyboard traps

### 10.4 Display
- [ ] Test with "Large" and "Largest" font size
- [ ] Test with High Contrast theme
- [ ] Test with Color Correction (Deuteranomaly, Protanomaly, Tritanomaly)
- [ ] Test with "Remove animations" enabled

---

## 11. Implementation Priority

### Phase 1: Core Infrastructure (Week 1)
1. High-Contrast themes
2. Touch target fixes
3. Semantic roles/labels on primary navigation

### Phase 2: Content Accessibility (Week 2)
1. MarkdownPreview WebView accessibility
2. Editor toolbar accessibility
3. Form labels and error announcements

### Phase 3: Polish & Testing (Week 3)
1. Skip navigation
2. Focus management
3. Screen reader announcements
4. Full testing pass

---

## 12. Files to Modify

### Core Theme System
- `app/src/main/java/com/example/ui/theme/Color.kt`
- `app/src/main/java/com/example/ui/theme/Theme.kt`
- `app/src/main/java/com/example/ui/theme/Type.kt`

### Main Screens
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`

### Components
- `app/src/main/java/com/example/ui/components/MarkdownPreview.kt`

### ViewModels (for announcements)
- `app/src/main/java/com/example/ui/viewmodel/VaultViewModel.kt`

### Resources
- `app/src/main/res/values/strings.xml` (accessibility strings)
- `app/src/main/AndroidManifest.xml` (if needed for accessibility service)