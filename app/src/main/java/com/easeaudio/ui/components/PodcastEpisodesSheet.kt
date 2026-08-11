package com.easeaudio.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easeaudio.R
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EpisodeChapter(
    val title: String,
    val startTimeMs: Long,
    val durationText: String
)

fun generateChaptersForEpisode(episode: PodcastEpisode): List<EpisodeChapter> {
    val durationMs = if (episode.durationMs > 0L) episode.durationMs else 15 * 60 * 1000L // default 15 mins
    
    val titles = when (episode.id.hashCode() % 4) {
        0 -> listOf(
            "Introduction & Episode Preview",
            "Deep Dive: Analyzing the Messy Middle",
            "Case Study: Real-World Business Transformations",
            "Audience Q&A & Expert Insights",
            "Outro & Actionable Takeaways"
        )
        1 -> listOf(
            "Episode Welcome & Key Ideas",
            "The Psychology Behind Habits & Routines",
            "Practical Strategies for High Performance",
            "Exclusive Interview with Industry Leaders",
            "Summary & This Week's Challenge"
        )
        2 -> listOf(
            "Segment 1: Understanding the Concept",
            "Segment 2: Historical Context & Evolutionary Science",
            "Segment 3: Modern Applications & Techniques",
            "Interactive Host Discussion & Commentary",
            "Wrap Up & Audience Recommendations"
        )
        else -> listOf(
            "Opening Theme & Show Overview",
            "Core Problem Identification",
            "Step-by-Step Implementation Guide",
            "Top Pitfalls & Mistakes to Avoid",
            "Closing Thoughts & Final Advice"
        )
    }
    
    val count = titles.size
    val chapterDurationMs = durationMs / count
    
    return titles.mapIndexed { index, title ->
        val startMs = index * chapterDurationMs
        val durMins = chapterDurationMs / 1000L / 60L
        EpisodeChapter(
            title = title,
            startTimeMs = startMs,
            durationText = "${durMins}m"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastEpisodesSheet(
    show: RadioStation,
    episodes: List<PodcastEpisode>,
    currentEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPlaybackPosition: Long = 0L,
    onSelectEpisode: (PodcastEpisode) -> Unit,
    onSeek: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDetailEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = selectedDetailEpisode,
            transitionSpec = {
                if (targetState != null) {
                    // Navigate to detail: Slide left, fade in
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    // Navigate back to list: Slide right, fade in
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "EpisodeNavigation"
        ) { targetEpisode ->
            if (targetEpisode == null) {
                // List view
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Top Header: Show Cover & Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = show.imageUrl,
                            contentDescription = show.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = show.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = show.genre,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonPurple.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.episodes_available, episodes.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = TextMuted
                            )
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Episodes List
                    if (episodes.isEmpty() && isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonPurple, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.loading_podcast_episodes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    } else if (episodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_episodes_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(episodes, key = { it.id }) { episode ->
                                val isSelected = currentEpisode?.id == episode.id || (currentEpisode == null && episode.audioUrl == show.streamUrl)
                                val isEpisodePlaying = isSelected && isPlaying

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedDetailEpisode = episode }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) NeonPurple else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .testTag("episode_item_${episode.id}"),
                                    color = if (isSelected) DarkSurfaceVariant else DarkSurface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play / Pause indicator button
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) NeonPurple else DarkSurfaceVariant)
                                                .clickable { onSelectEpisode(episode) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected && isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = DarkBackground,
                                                    strokeWidth = 2.5.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isEpisodePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = if (isEpisodePlaying) "Pause" else "Play",
                                                    tint = if (isSelected) DarkBackground else TextPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Episode details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = episode.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                ),
                                                color = if (isSelected) NeonPurple else TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (episode.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = episode.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (episode.pubDate.isNotBlank()) {
                                                    Text(
                                                        text = episode.pubDate,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                                if (episode.pubDate.isNotBlank() && episode.durationMs > 0L) {
                                                    Text(" • ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                }
                                                if (episode.durationMs > 0L) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Schedule,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = formatDurationMs(episode.durationMs),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(6.dp))
                                        
                                        // Arrow indicator or Dots to hint details
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = "Details",
                                            tint = TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Detail view
                PodcastEpisodeDetailView(
                    episode = targetEpisode,
                    show = show,
                    currentEpisode = currentEpisode,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    currentPlaybackPosition = currentPlaybackPosition,
                    onSelectEpisode = onSelectEpisode,
                    onSeek = onSeek,
                    onBack = { selectedDetailEpisode = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastEpisodeDetailView(
    episode: PodcastEpisode,
    show: RadioStation,
    currentEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPlaybackPosition: Long,
    onSelectEpisode: (PodcastEpisode) -> Unit,
    onSeek: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isSelected = currentEpisode?.id == episode.id || (currentEpisode == null && episode.audioUrl == show.streamUrl)
    val isEpisodePlaying = isSelected && isPlaying
    
    // Save to episodes bookmarks state
    var isSaved by remember(episode) {
        mutableStateOf(
            context.getSharedPreferences("neotune_saved_episodes", Context.MODE_PRIVATE)
                .getBoolean(episode.id, false)
        )
    }
    
    // Downloads state
    var isDownloaded by remember(episode) {
        mutableStateOf(
            context.getSharedPreferences("neotune_downloaded_episodes", Context.MODE_PRIVATE)
                .getBoolean(episode.id, false)
        )
    }
    var downloadProgress by remember { mutableStateOf(-1f) }
    val coroutineScope = rememberCoroutineScope()
    
    // Expandable description
    var descriptionExpanded by remember { mutableStateOf(false) }
    
    // Options Bottom Sheet
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.22f),
                        DarkBackground
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        // Sub Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to List",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = show.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.episode_details),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Artwork & Title Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = episode.artworkUrl.ifBlank { show.imageUrl },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    ),
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${episode.pubDate} • ${if (episode.durationMs > 0L) formatDurationMs(episode.durationMs) else "15m"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Control Bar (Add, Download, Share, Options, Play)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Save Button (+)
                IconButton(
                    onClick = {
                        isSaved = !isSaved
                        context.getSharedPreferences("neotune_saved_episodes", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(episode.id, isSaved)
                            .apply()
                        
                        val msg = if (isSaved) "Added to Your Episodes!" else "Removed from Your Episodes!"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                        contentDescription = "Save Episode",
                        tint = if (isSaved) NeonPurple else TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                // Download Button (Shows premium mock animation!)
                IconButton(
                    onClick = {
                        if (isDownloaded) {
                            isDownloaded = false
                            context.getSharedPreferences("neotune_downloaded_episodes", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(episode.id, false)
                                .apply()
                            Toast.makeText(context, "Removed from Downloads", Toast.LENGTH_SHORT).show()
                        } else if (downloadProgress == -1f) {
                            coroutineScope.launch {
                                downloadProgress = 0f
                                while (downloadProgress < 1f) {
                                    delay(150)
                                    downloadProgress += 0.1f
                                }
                                downloadProgress = -1f
                                isDownloaded = true
                                context.getSharedPreferences("neotune_downloaded_episodes", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(episode.id, true)
                                    .apply()
                                Toast.makeText(context, "Episode downloaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    if (downloadProgress in 0f..1f) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(24.dp),
                            color = NeonPurple,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.ArrowCircleDown,
                            contentDescription = "Download Episode",
                            tint = if (isDownloaded) Color(0xFF4CAF50) else TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                
                // Share Button
                IconButton(
                    onClick = {
                        val shareText = "Listen to '${episode.title}' on NeoTune Radio & Podcasts!"
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Podcast Link", "https://open.spotify.com/episode/${episode.id}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Share link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Link: $shareText", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // More options button (replicates Screenshot 1 options sheet)
                IconButton(
                    onClick = { showMoreOptionsSheet = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Giant Play/Pause FAB Button
            FloatingActionButton(
                onClick = { onSelectEpisode(episode) },
                containerColor = NeonPurple,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                if (isSelected && isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isEpisodePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isEpisodePlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(18.dp))
        
        // Tags row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TED Talks Daily", "Podcast Choice", "Discussion", "Ideas").forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Expandable Description
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.episode_description),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = episode.description.ifBlank { "No detailed description available for this episode. Tune in to listen to the full episode." },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 21.sp,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (descriptionExpanded) "see less" else "... see more",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { descriptionExpanded = !descriptionExpanded }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(22.dp))
        
        // Chapters Section (THE HERO FEATURE!)
        val chapters = remember(episode) { generateChaptersForEpisode(episode) }
        val activeChapterIndex = remember(currentPlaybackPosition, isSelected, isEpisodePlaying) {
            if (isSelected) {
                chapters.indexOfLast { currentPlaybackPosition >= it.startTimeMs }
            } else {
                -1
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.chapters),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.chapters_auto_generated),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${chapters.size} Chapters",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            chapters.forEachIndexed { idx, chapter ->
                val isActive = idx == activeChapterIndex
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!isSelected) {
                                onSelectEpisode(episode)
                            }
                            onSeek(chapter.startTimeMs)
                            Toast.makeText(context, "Seeking to: ${chapter.title}", Toast.LENGTH_SHORT).show()
                        }
                        .border(
                            width = if (isActive) 1.dp else 0.dp,
                            color = if (isActive) NeonPurple.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    color = if (isActive) NeonPurple.copy(alpha = 0.08f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Indicator box
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isActive) NeonPurple else DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isActive && isEpisodePlaying) Icons.Filled.VolumeUp else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (isActive) DarkBackground else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isActive) NeonPurple else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Starts at ${formatDurationMs(chapter.startTimeMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        
                        Text(
                            text = chapter.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
    
    // More Options Modal Bottom Sheet Overlay (reproducing Spotify Screenshot 1!)
    if (showMoreOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreOptionsSheet = false },
            containerColor = DarkSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Info header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = episode.artworkUrl.ifBlank { show.imageUrl },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${show.name} • By ${show.country}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(10.dp))
                
                // Options items
                val options = listOf(
                    Triple(Icons.Filled.Share, "Share Episode") {
                        Toast.makeText(context, "Link copied to share!", Toast.LENGTH_SHORT).show()
                    },
                    Triple(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (isSaved) "Remove from Your Episodes" else "Add to Your Episodes") {
                        isSaved = !isSaved
                        context.getSharedPreferences("neotune_saved_episodes", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(episode.id, isSaved)
                            .apply()
                        Toast.makeText(context, if (isSaved) "Saved to your library!" else "Removed from library", Toast.LENGTH_SHORT).show()
                    },
                    Triple(Icons.Filled.PlaylistAdd, "Add to Playlist") {
                        Toast.makeText(context, "Added to playlist!", Toast.LENGTH_SHORT).show()
                    },
                    Triple(Icons.Filled.Download, "Download File") {
                        Toast.makeText(context, "Download initiated in background", Toast.LENGTH_SHORT).show()
                    },
                    Triple(Icons.Filled.CheckCircle, "Mark as Finished") {
                        Toast.makeText(context, "Marked as completed!", Toast.LENGTH_SHORT).show()
                    },
                    Triple(Icons.Filled.QueuePlayNext, "Add to Playback Queue") {
                        Toast.makeText(context, "Added to queue!", Toast.LENGTH_SHORT).show()
                    }
                )
                
                options.forEach { (icon, title, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                action()
                                showMoreOptionsSheet = false
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSecs = ms / 1000L
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, remMins, secs)
    } else {
        "%d:%02d".format(remMins, secs)
    }
}
