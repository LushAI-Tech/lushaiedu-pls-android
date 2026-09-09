package com.lushaiedupls.ui.common

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandNavy
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val LegalCardShape = RoundedCornerShape(18.dp)
private val LegalHeroShape = RoundedCornerShape(22.dp)
private val LegalChipShape = RoundedCornerShape(50)

private sealed interface LegalContentBlock {
    data class Paragraph(val text: String) : LegalContentBlock
    data class LabelledParagraph(val label: String, val body: String) : LegalContentBlock
    data class Subheading(val text: String) : LegalContentBlock
    data class Bullet(val text: String) : LegalContentBlock
    data class Disclaimer(val text: String) : LegalContentBlock
}

private data class LegalSection(
    val number: Int,
    val title: String,
    val blocks: List<LegalContentBlock>,
)

private data class ParsedLegalDocument(
    val lastUpdated: String?,
    val intro: List<LegalContentBlock>,
    val sections: List<LegalSection>,
)

private fun parseLegalDocument(raw: String, screenTitle: String): ParsedLegalDocument {
    val lines = raw.lines()
    var lastUpdated: String? = null
    val intro = mutableListOf<LegalContentBlock>()
    val sections = mutableListOf<LegalSection>()
    var currentSection: LegalSection? = null

    fun flushSection() {
        currentSection?.let { section ->
            if (section.blocks.isNotEmpty() || section.title.isNotBlank()) {
                sections += section
            }
        }
        currentSection = null
    }

    fun addBlock(block: LegalContentBlock) {
        val section = currentSection
        if (section == null) {
            intro += block
        } else {
            currentSection = section.copy(blocks = section.blocks + block)
        }
    }

    lines.forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@forEach

        if (line.equals(screenTitle, ignoreCase = true)) return@forEach

        if (line.startsWith("Last updated:", ignoreCase = true)) {
            lastUpdated = line.removePrefix("Last updated:").trim()
            return@forEach
        }

        val sectionMatch = Regex("""^(\d+)\.\s+(.+)$""").matchEntire(line)
        if (sectionMatch != null) {
            flushSection()
            currentSection = LegalSection(
                number = sectionMatch.groupValues[1].toInt(),
                title = sectionMatch.groupValues[2].trim(),
                blocks = emptyList(),
            )
            return@forEach
        }

        if (line.startsWith("- ")) {
            addBlock(LegalContentBlock.Bullet(line.removePrefix("- ").trim()))
            return@forEach
        }

        if (line.length > 48 && line == line.uppercase() && line.any { it.isLetter() }) {
            addBlock(LegalContentBlock.Disclaimer(line))
            return@forEach
        }

        val labelledMatch = Regex("""^([^:]+):\s+(.+)$""").matchEntire(line)
        if (labelledMatch != null && !labelledMatch.groupValues[1].contains('.')) {
            addBlock(
                LegalContentBlock.LabelledParagraph(
                    label = labelledMatch.groupValues[1].trim(),
                    body = labelledMatch.groupValues[2].trim(),
                ),
            )
            return@forEach
        }

        if (line.endsWith(":") && line.length < 72) {
            addBlock(LegalContentBlock.Subheading(line.removeSuffix(":").trim()))
            return@forEach
        }

        addBlock(LegalContentBlock.Paragraph(line))
    }

    flushSection()
    return ParsedLegalDocument(lastUpdated = lastUpdated, intro = intro, sections = sections)
}

@Composable
fun LegalDocumentScreen(
    title: String,
    @RawRes bodyResId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rawBody = remember(bodyResId) {
        context.resources.openRawResource(bodyResId).bufferedReader().use { it.readText() }
    }
    val document = remember(rawBody, title) { parseLegalDocument(rawBody, title) }
    val headerIcon = if (title.contains("Privacy", ignoreCase = true)) {
        Icons.Outlined.Policy
    } else {
        Icons.AutoMirrored.Outlined.Article
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .statusBarsPadding(),
    ) {
        LegalTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            LegalHeroCard(
                title = title,
                lastUpdated = document.lastUpdated,
                icon = headerIcon,
            )
            document.intro.takeIf { it.isNotEmpty() }?.let { introBlocks ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LegalCardShape,
                    color = BgWhite,
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.legal_overview),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandNavy,
                        )
                        introBlocks.forEach { block ->
                            LegalContentBlockView(block = block)
                        }
                    }
                }
            }
            document.sections.forEach { section ->
                LegalSectionCard(section = section)
            }
        }
    }
}

@Composable
private fun LegalTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgWhite),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = BrandBlack,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                color = BrandBlack,
            )
        }
        HorizontalDivider(color = BorderGray.copy(alpha = 0.45f))
    }
}

@Composable
private fun LegalHeroCard(
    title: String,
    lastUpdated: String?,
    icon: ImageVector,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LegalHeroShape,
        color = BgWhite,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    ),
                    color = BrandBlack,
                )
                if (lastUpdated != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.legal_last_updated, lastUpdated),
                        modifier = Modifier
                            .clip(LegalChipShape)
                            .background(BrandNavy.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandNavy,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalSectionCard(section: LegalSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LegalCardShape,
        color = BgWhite,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandOrange),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = section.number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = BgWhite,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = section.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                    ),
                    color = BrandBlack,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                color = BorderGray.copy(alpha = 0.35f),
            )
            section.blocks.forEach { block ->
                LegalContentBlockView(block = block)
            }
        }
    }
}

@Composable
private fun LegalContentBlockView(block: LegalContentBlock) {
    when (block) {
        is LegalContentBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                ),
                color = TextSecondary,
            )
        }
        is LegalContentBlock.LabelledParagraph -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = block.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandBlack,
                )
                Text(
                    text = block.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                    ),
                    color = TextSecondary,
                )
            }
        }
        is LegalContentBlock.Subheading -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                ),
                color = BrandNavy,
            )
        }
        is LegalContentBlock.Bullet -> {
            Row(
                modifier = Modifier.padding(start = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrandOrange),
                )
                Text(
                    text = block.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                    ),
                    color = TextSecondary,
                )
            }
        }
        is LegalContentBlock.Disclaimer -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = BgLight,
            ) {
                Text(
                    text = block.text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = BrandBlack.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LegalDocumentScreenPreview() {
    LushAIEdu_PLSTheme {
        LegalDocumentScreen(
            title = "Privacy Policy",
            bodyResId = R.raw.privacy_policy,
            onBack = {},
        )
    }
}
