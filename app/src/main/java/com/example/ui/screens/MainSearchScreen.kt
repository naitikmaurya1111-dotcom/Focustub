package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lecture
import com.example.data.model.StudyCategory
import com.example.data.repository.ApiKeySource
import com.example.data.repository.ApiKeyStatus
import com.example.data.repository.EducationalVideoCatalog
import com.example.ui.components.LectureCard
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainSearchScreen(
    searchQuery: String,
    selectedCategory: String,
    searchResults: List<Lecture>,
    searchHistory: List<String>,
    isSearching: Boolean,
    isLiveApiSearch: Boolean,
    searchApiErrorMessage: String?,
    apiKeyStatus: ApiKeyStatus,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRetrySearch: () -> Unit,
    onDeleteSearchHistoryItem: (String) -> Unit,
    onClearAllSearchHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onLectureSelected: (Lecture) -> Unit,
    onToggleSaveLecture: (Lecture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    val clipboardText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
    val hasValidPasteLink = (clipboardText.contains("youtu.be") || clipboardText.contains("youtube.com") ||
            (clipboardText.length == 11 && clipboardText.matches(Regex("^[a-zA-Z0-9_-]{11}$"))))

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val searchBarScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = tween(300),
        label = "searchBarScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Academic Badge + Title + API Status Pill
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.White)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "FocusTube Gemini Brand",
                                    tint = FocusIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "FOCUSTUBE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = Color.White
                                )
                            )
                        }

                        // API Status Pill
                        val (pillBg, pillTextColor, pillText, pillIcon) = when {
                            isLiveApiSearch -> Quad(Color(0x2210B981), FocusEmerald, "YouTube API Live", Icons.Default.CloudDone)
                            apiKeyStatus.source != ApiKeySource.NONE -> Quad(Color(0x226366F1), FocusIndigo, "API Ready", Icons.Default.CloudDone)
                            else -> Quad(Color(0x2238BDF8), Color(0xFF38BDF8), "Academic Engine", Icons.Default.AutoAwesome)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(pillBg)
                                .clickable { onOpenSettings() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = pillIcon,
                                contentDescription = null,
                                tint = pillTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = pillText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = pillTextColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Clean Academic Study",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search for any university course or paste a lecture link to begin studying without feeds or algorithm distractions.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Search Bar Input
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = {
                            Text(
                                text = "Search lectures, courses, or professors...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (searchQuery.isNotBlank()) FocusIndigo else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotBlank(),
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    IconButton(
                                        onClick = {
                                            onClearSearch()
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.size(32.dp).testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            onSearchSubmitted(searchQuery)
                                            focusManager.clearFocus()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (searchQuery.isNotBlank()) FocusIndigo else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .testTag("submit_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Submit Search",
                                        tint = if (searchQuery.isNotBlank()) Color.White else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    onSearchSubmitted(searchQuery)
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard,
                            focusedBorderColor = FocusIndigo.copy(alpha = 0.5f),
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = FocusIndigo
                        ),
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(searchBarScale)
                            .testTag("main_search_input")
                    )

                    // Quick Paste Link Bar if valid clipboard item
                    AnimatedVisibility(
                        visible = hasValidPasteLink && searchQuery.isBlank(),
                        enter = slideInVertically(tween(300)) { -it / 2 } + fadeIn(tween(300))
                    ) {
                        Card(
                            onClick = {
                                onSearchSubmitted(clipboardText)
                                focusManager.clearFocus()
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0x336366F1)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FocusIndigo),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = FocusIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Paste & Study Link: $clipboardText",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // CLEAN HOME STATE: When no search query has been entered
            if (searchQuery.isBlank()) {
                // Persistent Search History Section
                if (searchHistory.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = FocusIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "RECENT SEARCHES",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FocusIndigo,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }

                                TextButton(
                                    onClick = onClearAllSearchHistory,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear History",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                    )
                                }
                            }

                            // History chips flow
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                searchHistory.forEach { historyQuery ->
                                    Surface(
                                        onClick = {
                                            onSearchSubmitted(historyQuery)
                                            focusManager.clearFocus()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = SlateCard,
                                        border = BorderStroke(1.dp, FocusIndigo.copy(alpha = 0.2f)),
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = FocusIndigo.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = historyQuery,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            IconButton(
                                                onClick = { onDeleteSearchHistoryItem(historyQuery) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove from history",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Premium Illustration Area
                item {
                    val textGradient = Brush.linearGradient(
                        colors = listOf(FocusIndigo, FocusEmerald)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
                                    )
                                )
                                .border(BorderStroke(1.5.dp, Color(0x33FFFFFF)), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "FocusTube Gemini Spark",
                                tint = FocusIndigo,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Zero Feeds. Complete Academic Focus.",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Unspecified,
                            modifier = Modifier.graphicsLayer(alpha = 0.99f)
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = textGradient,
                                            blendMode = BlendMode.SrcAtop
                                        )
                                    }
                                },
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Search above for any lecture, syllabus topic, or paste a video link to begin taking timestamped notes with full focus tools.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 22.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                // SEARCH ACTIVE STATE: Category filter chips + Search Results List
                item {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 16.dp)
                            ) {
                                items(EducationalVideoCatalog.CATEGORIES) { category ->
                                    val isSelected = selectedCategory == category.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onCategorySelected(category.id) },
                                        label = {
                                            Text(
                                                text = "${category.icon} ${category.name}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else TextSecondary
                                                )
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FocusIndigo,
                                            containerColor = SlateCard
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) FocusIndigo else SlateBorder
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .testTag("category_chip_${category.id}")
                                            .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(10.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Error State
                if (searchApiErrorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AF43F5E)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x4DF43F5E))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = searchApiErrorMessage,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFF43F5E))
                                )
                            }
                        }
                    }
                }

                // Results Section Header
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Search Results for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            AnimatedContent(
                                targetState = searchResults.size,
                                label = "count_anim"
                            ) { count ->
                                Text(
                                    text = "$count lectures",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SlateBorder)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Loading State
                if (isSearching) {
                    item {
                        val infiniteTransition = rememberInfiniteTransition(label = "loading")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = InfiniteRepeatableSpec(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "loadingAlpha"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Loading",
                                    tint = FocusIndigo.copy(alpha = alpha),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Searching Academic Catalog...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = FocusIndigo.copy(alpha = alpha),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                } else if (searchResults.isEmpty() && searchApiErrorMessage == null) {
                    // Empty Results State
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No lectures found for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching for another topic or paste a direct YouTube video link.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onClearSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Search", color = Color.White)
                            }
                        }
                    }
                } else {
                    // Results List
                    items(searchResults, key = { it.videoId }) { lecture ->
                        LectureCard(
                            lecture = lecture,
                            onClick = { onLectureSelected(lecture) },
                            onToggleSave = { onToggleSaveLecture(lecture) },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
