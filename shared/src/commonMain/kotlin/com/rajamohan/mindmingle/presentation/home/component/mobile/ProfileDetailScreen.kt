package com.rajamohan.mindmingle.presentation.home.component.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.domain.model.ProfileOptionsRepository
import com.rajamohan.mindmingle.domain.model.GeoDistance
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.BoltIcon
import com.rajamohan.mindmingle.presentation.common.icon.ExternalLinkIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing

/**
 * Everything a profile holds, opened by tapping a card in the deck.
 *
 * The card can only ever show a headline — one photo, a bio and six interests — because it has to
 * stay swipeable. This screen is the other half of that trade: every photo, and every answer the
 * person gave during profile setup.
 *
 * ## The spacing
 * Sections are separated by [SectionGap] and the page ends on [PageEndGap], both far larger than
 * the card's. That openness is the whole design: this is the screen someone reads before deciding,
 * so it is paced to be read rather than scanned, and a dense wall of chips would undo the point of
 * leaving the deck. Everything is one column on a [ContentMaxWidth] measure, so the line length
 * stays comfortable on a tablet or a resized desktop window instead of stretching edge to edge.
 *
 * It reads the [User] the deck already loaded, so opening it costs no reads. The lifestyle answers
 * live in `details`/`selections` under opaque keys, turned into their human labels with the same
 * `profile_options.json` the setup flow was driven by; a key missing from that file is skipped
 * rather than shown raw.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileDetailScreen(
    profile: User,
    distanceKm: Double?,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    // key -> label, loaded once from the bundled options file. Empty until it arrives, which only
    // costs the lifestyle block a frame; everything above it renders immediately.
    var fieldLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Hoisted so the gallery at the foot of the page and the hero at the top are the same
    // selection — tapping a thumbnail scrolls nothing, it changes the picture already on screen.
    var photoIndex by remember(profile.uid) { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        fieldLabels = ProfileOptionsRepository.getSections()
            .flatMap { section -> section.fields }
            .associate { field -> field.key to field.label }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailPhotoPager(
                    profile = profile,
                    photoIndex = photoIndex,
                    onPhotoIndexChange = { photoIndex = it }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = ContentMaxWidth)
                        .padding(horizontal = Spacing.screenHorizontal)
                ) {
                    IdentityBlock(profile = profile, distanceKm = distanceKm)

                    if (profile.bio.isNotBlank()) {
                        DetailSection(title = "About") {
                            Text(
                                text = profile.bio,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                lineHeight = 26.sp
                            )
                        }
                    }

                    if (profile.interests.isNotEmpty()) {
                        DetailSection(title = "Interests") {
                            // Tinted, unlike the neutral chips a lifestyle answer uses. Interests
                            // are the thing to open a conversation with, so they are the one list
                            // on the page that is allowed to carry colour.
                            ChipFlow(values = profile.interests, accent = true)
                        }
                    }

                    // Both answer shapes from profile setup: single-select lands in `details`,
                    // multi-select in `selections`. Blank answers are dropped — an empty row reads
                    // as a broken field rather than as "not answered".
                    val answers = buildList {
                        profile.details.forEach { (key, value) ->
                            val label = fieldLabels[key]
                            if (label != null && value.isNotBlank()) add(label to listOf(value))
                        }
                        profile.selections.forEach { (key, values) ->
                            val label = fieldLabels[key]
                            val cleaned = values.filter { it.isNotBlank() }
                            if (label != null && cleaned.isNotEmpty()) add(label to cleaned)
                        }
                    }
                    if (answers.isNotEmpty()) {
                        val firstName = profile.name.substringBefore(' ').ifBlank { "them" }
                        DetailSection(title = "More about $firstName") {
                            AnswerGrid(answers = answers)
                        }
                    }

                    val links = buildList {
                        if (profile.githubUrl.isNotBlank()) add(profile.githubUrl)
                        addAll(profile.portfolioLinks.filter { it.isNotBlank() })
                    }
                    if (links.isNotEmpty()) {
                        DetailSection(title = "Links") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                links.forEach { link -> LinkRow(link = link) }
                            }
                        }
                    }

                    if (profile.displayPhotoUrls.size > 1) {
                        DetailSection(title = "Photos") {
                            PhotoGallery(
                                photos = profile.displayPhotoUrls,
                                uid = profile.uid,
                                activeIndex = photoIndex,
                                onSelect = { photoIndex = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(PageEndGap))
                }
            }

            // Floats over the photo rather than sitting in a bar: the photo is full-bleed, and a
            // solid app bar above it would cost the top fifth of the first image.
            // Floats above the scroll, so it has to read against a photo and against the page
            // background both. A translucent black pill worked over the hero and then sat on top
            // of the answers as a smudge; an opaque surface chip with a shadow reads as a control
            // wherever the page has been scrolled to.
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = colors.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(Spacing.screenVertical)
                    .size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** The widest the text column is allowed to get, so a long bio never runs edge to edge on a tablet. */
private val ContentMaxWidth = 620.dp

/** Space above a section heading. Deliberately large — it is what separates one idea from the next. */
private val SectionGap = 40.dp

/** Between a heading and its content. */
private val HeadingGap = 16.dp

/** Between two rows of fact cards. */
private val AnswerGap = 12.dp

/** Breathing room under the last section, so the final line never sits on the screen edge. */
private val PageEndGap = 56.dp

/**
 * Name, age, badge and the two or three facts worth knowing before reading anything else.
 *
 * It overlaps the photo above it by a corner radius, which is what makes the photo read as part of
 * the page rather than as a banner stuck on top of it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityBlock(profile: User, distanceKm: Double?) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Spacer(modifier = Modifier.height(28.dp))

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = profile.name.ifBlank { "Developer" },
            style = typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (profile.isPremium) {
            Spacer(modifier = Modifier.width(8.dp))
            PremiumBadge(size = 22.dp, modifier = Modifier.padding(bottom = 5.dp))
        }
        if (profile.age > 0) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = profile.age.toString(),
                style = typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color = colors.onSurfaceVariant
            )
        }
    }

    // Same line as the card, one size up — someone who opened the full profile is deciding, and
    // this is the fact they are deciding on.
    DistanceLine(
        distanceKm = distanceKm,
        fallbackLocation = profile.location,
        style = typography.bodyMedium,
        iconSize = 14.dp
    )

    if (profile.occupation.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = profile.occupation,
            style = typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant
        )
    }

    val pills = buildList {
        if (profile.lookingFor.isNotBlank()) add(profile.lookingFor to true)
        if (profile.experienceLevel.isNotBlank()) add(profile.experienceLevel to false)
    }
    if (pills.isNotEmpty()) {
        Spacer(modifier = Modifier.height(20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            pills.forEach { (text, isIntent) ->
                IconPill(text = text) { tint ->
                    if (isIntent) {
                        SparkleBurstIcon(color = tint, modifier = Modifier.size(14.dp))
                    } else {
                        BoltIcon(color = tint, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * The photo strip. Tapping the left or right half steps through, the same gesture the deck card
 * uses — the screen scrolls vertically, so a horizontal pager here would be safe, but keeping the
 * two surfaces identical means one thing to learn rather than two.
 */
@Composable
private fun DetailPhotoPager(
    profile: User,
    photoIndex: Int,
    onPhotoIndexChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val photos = profile.displayPhotoUrls

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
    ) {
        RemoteProfileImage(
            url = photos.getOrNull(photoIndex).orEmpty(),
            uid = profile.uid,
            contentDescription = profile.name.ifBlank { "Profile photo" },
            placeholderIconSize = 96.dp,
            modifier = Modifier.fillMaxSize()
        )

        // Top scrim keeps the back button and photo dots legible over a pale photo; the bottom one
        // lands the image on the page background instead of on a hard colour edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.28f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to colors.background
                    )
                )
        )

        if (photos.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(profile.uid, photos.size) {
                        detectTapGestures { offset ->
                            onPhotoIndexChange(
                                if (offset.x < size.width / 2f) {
                                    (photoIndex - 1 + photos.size) % photos.size
                                } else {
                                    (photoIndex + 1) % photos.size
                                }
                            )
                        }
                    }
            )

            PhotoProgressBar(
                count = photos.size,
                activeIndex = photoIndex,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    // Clears the floating back button, which sits in the same corner.
                    .padding(start = 76.dp, end = Spacing.screenHorizontal, top = 28.dp)
            )
        }
    }
}

/**
 * Every photo as a thumbnail, driving the hero at the top of the page.
 *
 * The hero can only show one at a time and stepping through it means tapping blind, which is fine
 * on a card in the deck and not fine on the screen someone opened to look properly. The whole set
 * laid out here is also the honest answer to "how many photos does this person actually have".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoGallery(
    photos: List<String>,
    uid: String,
    activeIndex: Int,
    onSelect: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        photos.forEachIndexed { index, url ->
            val isActive = index == activeIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(16.dp),
                // The one on screen is called out with a ring rather than by dimming the others:
                // dimming a photo someone is deciding on is the wrong thing to do to it.
                border = if (isActive) BorderStroke(2.dp, colors.primary) else null,
                color = colors.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(GalleryThumbSize)
            ) {
                RemoteProfileImage(
                    url = url,
                    uid = uid,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isActive) 3.dp else 0.dp)
                        .clip(RoundedCornerShape(if (isActive) 13.dp else 16.dp))
                )
            }
        }
    }
}

/** Big enough to tell two photos of the same person apart, small enough for three to a row. */
private val GalleryThumbSize = 96.dp

/**
 * The lifestyle answers, as a grid of fact cards.
 *
 * These were a vertical stack of "LABEL over one chip", which at a dozen answers turned the page
 * into a column of near-identical rows a screen and a half long — every answer the same shape and
 * the same weight, so none of them read. Two things fix that.
 *
 * The first is pairing: a one-word answer ("Yes", "Google", "Night owl") needs nothing like a full
 * line, so single-value answers go two to a row and the section collapses to half its height.
 *
 * The second is that a multi-value answer — languages spoken, say — keeps the full width and shows
 * its values as chips. The difference in shape is the point: the eye can now tell a one-word fact
 * from a list at a glance instead of reading every label to find out.
 */
@Composable
private fun AnswerGrid(answers: List<Pair<String, List<String>>>) {
    val single = answers.filter { (_, values) -> values.size == 1 }
    val multi = answers.filter { (_, values) -> values.size > 1 }

    Column(verticalArrangement = Arrangement.spacedBy(AnswerGap)) {
        single.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AnswerGap)) {
                row.forEach { (label, values) ->
                    FactCard(
                        label = label,
                        value = values.first(),
                        modifier = Modifier.weight(1f)
                    )
                }
                // An odd count leaves the last card half-width rather than stretched across the
                // row, so the grid keeps its column even when the answers do not divide evenly.
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        multi.forEach { (label, values) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                FieldLabel(text = label)
                Spacer(modifier = Modifier.height(10.dp))
                ChipFlow(values = values)
            }
        }
    }
}

@Composable
private fun FactCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            FieldLabel(text = label)
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The small caps label above a fact. Same treatment everywhere, so the grid reads as one system. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

/**
 * A titled block. The rule under the heading is what carries the "premium" read at this spacing —
 * with gaps this large, headings alone start to float free of the text they belong to.
 */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Spacer(modifier = Modifier.height(SectionGap))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.onSurface
    )
    Spacer(modifier = Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .width(34.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.primary.copy(alpha = 0.55f))
    )
    Spacer(modifier = Modifier.height(HeadingGap))
    content()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(values: List<String>, accent: Boolean = false) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        values.forEach { value -> DetailChip(text = value, accent = accent) }
    }
}

@Composable
private fun DetailChip(text: String, accent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (accent) colors.primaryContainer.copy(alpha = 0.55f) else colors.surfaceVariant.copy(alpha = 0.6f),
        border = if (accent) null else BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            color = if (accent) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun LinkRow(link: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            ExternalLinkIcon(color = colors.primary, modifier = Modifier.size(16.dp))
            Text(
                text = link,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IconPill(text: String, icon: @Composable (Color) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.primaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            icon(colors.primary)
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}
