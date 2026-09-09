package com.lushaiedupls.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.splash.LocalBrandAnchors
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun LushAiEduWordmark(
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 32,
) {
    val brandAnchors = LocalBrandAnchors.current
    // "AI" in brand orange; full wordmark reads LushAIEdu
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                ),
            ) {
                append("Lush")
            }
            withStyle(
                SpanStyle(
                    color = BrandOrange,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                ),
            ) {
                append("AI")
            }
            withStyle(
                SpanStyle(
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                ),
            ) {
                append("Edu")
            }
        },
        fontSize = fontSizeSp.sp,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.5).sp,
        modifier = modifier.onGloballyPositioned { coords ->
            brandAnchors?.wordmarkBounds = coords.boundsInWindow()
        },
    )
}

@Composable
fun LushAiEduBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 112.dp,
    showLogo: Boolean = true,
    showAtomOrbit: Boolean = false,
    subtitle: String? = null,
    titleFontSizeSp: Int = 32,
    subtitleFontSizeSp: Int = 16,
) {
    // Orbit mark uses the full requested size; static logo stays slightly tighter.
    val displayedLogoSize = if (showAtomOrbit) logoSize else logoSize * 0.85f
    val brandAnchors = LocalBrandAnchors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showLogo) {

            if (showAtomOrbit) {
                AtomOrbitLogo(logoSize = displayedLogoSize)
            } else {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier
                        .size(displayedLogoSize)
                        .onGloballyPositioned { coords ->
                            brandAnchors?.logoBounds = coords.boundsInWindow()
                        },
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(modifier = Modifier.height(if (showAtomOrbit) 16.dp else 12.dp))
        }
        LushAiEduWordmark(fontSizeSp = titleFontSizeSp)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = BrandBlack,
                fontSize = subtitleFontSizeSp.sp,
                textAlign = TextAlign.Center,
                lineHeight = (subtitleFontSizeSp + 6).sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
fun PoweredByFooter(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.powered_by),
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Image(
            painter = painterResource(R.drawable.img_powered_by),
            contentDescription = stringResource(R.string.powered_by_lushaitech_cd),
            modifier = Modifier
                .width(112.dp)
                .height(28.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
