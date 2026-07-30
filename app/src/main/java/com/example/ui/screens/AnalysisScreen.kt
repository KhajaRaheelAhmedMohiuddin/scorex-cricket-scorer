package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Delivery
import com.example.model.ExtraType
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel
import kotlin.math.max

@Composable
fun AnalysisScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val match by viewModel.activeMatch.collectAsState()
    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active match for analysis", color = CoolSlate)
        }
        return
    }

    val activeMatch = match!!
    val deliveries = activeMatch.deliveries
    val totalOvers = activeMatch.selectedOvers

    // Pager state for charts slider
    val pagerState = rememberPagerState(pageCount = { 2 })

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Symmetric Header Architecture: Professional Sports-Broadcast Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            // Surfaced Navigation Tile on the left
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBgSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        if (activeMatch.status == "COMPLETED") {
                            viewModel.navigateTo(AppScreen.Summary)
                        } else {
                            viewModel.navigateTo(AppScreen.Scoring)
                        }
                    }
                    .testTag("analysis_back_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Centered Editorial Analytics Heads
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ANALYTICS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = StadiumGreen,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "REAL-TIME PERFORMANCE COCKPIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGrey,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Immersive Visualization Container ---
        val activePage = pagerState.currentPage
        val containerBorderColor = (if (activePage == 0) InfoTeal else StadiumGreen).copy(alpha = 0.20f)
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth().testTag("trajectory_card"),
            borderColor = containerBorderColor,
            backgroundColor = Color.Transparent
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Centered Complete Graph Card Title and Slider Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MATCH PERFORMANCE & TRAJECTORY",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp,
                        color = CleanWhite,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (activePage == 0) "SCORE PROGRESSION" else "OVER-WISE RUN RATE",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activePage == 0) InfoTeal else StadiumGreen,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )

                    // Pager dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = if (activePage == 0) 16.dp else 6.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (activePage == 0) InfoTeal else MutedGrey.copy(alpha = 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = if (activePage == 1) 16.dp else 6.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (activePage == 1) StadiumGreen else MutedGrey.copy(alpha = 0.4f))
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (deliveries.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = "Standby Mode",
                                    tint = StadiumGreen.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "AWAITING MATCH PROGRESSION DATA",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = DarkTextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Scored balls will dynamically update the cockpit trajectory charts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedGrey,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            if (page == 0) {
                                WormChartCanvas(
                                    deliveries = deliveries,
                                    totalOvers = totalOvers,
                                    teamAName = activeMatch.firstInningsBattingTeam.toAbbreviation(),
                                    teamBName = (if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA).toAbbreviation()
                                )
                            } else {
                                RunRateBarCanvas(
                                    deliveries = deliveries,
                                    totalOvers = totalOvers,
                                    teamAName = activeMatch.firstInningsBattingTeam.toAbbreviation(),
                                    teamBName = (if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA).toAbbreviation()
                                )
                            }
                        }
                    }
                }

                // Integrated Legend Dock nested at the bottom of the card block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBgMain.copy(alpha = 0.50f))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegendItem(
                            color = InfoTeal,
                            isWicket = false,
                            label = "${activeMatch.firstInningsBattingTeam.toAbbreviation()} (INN 1)"
                        )

                        ChartLegendItem(
                            color = GoldAccent,
                            isWicket = false,
                            label = "${(if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA).toAbbreviation()} (INN 2)"
                        )

                        ChartLegendItem(
                            color = WicketCrimson,
                            isWicket = true,
                            label = "WICKETS"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Comparative Trajectory Summary: High-Contrast Run-Rate Trajectory Section ---
        val deliveriesList = match?.deliveries ?: emptyList()
        val inn1Summary = remember(deliveriesList) { viewModel.getInnings1Summary() }
        val inn2Summary = remember(deliveriesList) { viewModel.getInnings2Summary() }

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorder,
            backgroundColor = DarkBgSurface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "RUN-RATE TRAJECTORY",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = CleanWhite,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Innings 1 (InfoTeal, Left-aligned)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "${activeMatch.firstInningsBattingTeam.toAbbreviation()} • INN 1",
                            fontSize = 10.sp,
                            color = MutedGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.2f RPO", inn1Summary.runRate),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = InfoTeal
                        )
                    }

                    // Vertical Separator
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(GlassBorder.copy(alpha = 0.40f))
                    )

                    // Right Column: Innings 2 (GoldAccent, Right-aligned)
                    val has2 = activeMatch.currentInnings == 2 || activeMatch.status == "COMPLETED"
                    val inn2TeamAbbr = (if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA).toAbbreviation()
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$inn2TeamAbbr • INN 2",
                            fontSize = 10.sp,
                            color = MutedGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (has2) String.format("%.2f RPO", inn2Summary.runRate) else "NOT STARTED",
                            fontSize = if (has2) 18.sp else 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (has2) GoldAccent else DarkTextMuted
                        )
                    }
                }
            }
        }
    }
}

// Custom AnalysisTabButton component for State-Driven Tactical Tabs
@Composable
fun AnalysisTabButton(
    active: Boolean,
    label: String,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) activeColor.copy(alpha = 0.15f) else DarkBgSurface)
            .border(
                BorderStroke(
                    width = if (active) 1.5.dp else 1.0.dp,
                    color = if (active) activeColor else GlassBorder
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
            color = if (active) activeColor else MutedGrey,
            letterSpacing = 1.sp
        )
    }
}

// Custom ChartLegendItem Component
@Composable
fun ChartLegendItem(
    color: Color,
    isWicket: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isWicket) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.2.dp, CleanWhite, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = CleanWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

// Border creator helper
@Composable
private fun BoxBorder(active: Boolean, color: Color): BorderStroke {
    return if (active) BorderStroke(1.dp, color) else BorderStroke(0.5.dp, GlassBorder)
}

// Cumulative run progression chart
@Composable
fun WormChartCanvas(
    deliveries: List<Delivery>,
    totalOvers: Int,
    teamAName: String,
    teamBName: String
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("worm_chart_canvas")
    ) {
        val width = size.width
        val height = size.height

        val paddingLeft = 65f
        val paddingRight = 65f
        val paddingTop = 65f
        val paddingBottom = 65f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        // Draw gray background for the plot area
        drawRect(
            color = DarkBgSurface,
            topLeft = Offset(paddingLeft, paddingTop),
            size = Size(graphWidth, graphHeight)
        )

        val densityVal = this.density
        val textPaintRight = Paint().apply {
            color = CoolSlate.copy(alpha = 0.85f).toArgb()
            textSize = 9f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val textPaintCenter = Paint().apply {
            color = CoolSlate.copy(alpha = 0.85f).toArgb()
            textSize = 9f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = MutedGrey.toArgb()
            textSize = 9f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        // Draw grid lines
        val oversToDraw = max(1, totalOvers)
        val stepX = graphWidth / oversToDraw

        // Horizontal Grid lines for Runs
        // Let's find maximum runs to scale Y
        val inn1D = deliveries.filter { it.innings == 1 }.sortedBy { it.timestamp }
        val inn2D = deliveries.filter { it.innings == 2 }.sortedBy { it.timestamp }

        var runs1 = 0
        val trajectory1 = mutableListOf<Pair<Float, Float>>() // X, Y scaled
        val wickets1 = mutableListOf<Pair<Float, Float>>() // scaled points where wickets fell

        var legalBalls1 = 0
        inn1D.forEach { d ->
            runs1 += d.runsBat + d.runsExtra
            if (d.extraType != ExtraType.WIDE && d.extraType != ExtraType.NO_BALL) {
                legalBalls1++
            }
            // we plot at fraction of overs
            val fractionOvers = legalBalls1.toFloat() / 6f
            trajectory1.add(fractionOvers to runs1.toFloat())
            if (d.wicket) {
                wickets1.add(fractionOvers to runs1.toFloat())
            }
        }

        var runs2 = 0
        val trajectory2 = mutableListOf<Pair<Float, Float>>()
        val wickets2 = mutableListOf<Pair<Float, Float>>()

        var legalBalls2 = 0
        inn2D.forEach { d ->
            runs2 += d.runsBat + d.runsExtra
            if (d.extraType != ExtraType.WIDE && d.extraType != ExtraType.NO_BALL) {
                legalBalls2++
            }
            val fractionOvers = legalBalls2.toFloat() / 6f
            trajectory2.add(fractionOvers to runs2.toFloat())
            if (d.wicket) {
                wickets2.add(fractionOvers to runs2.toFloat())
            }
        }

        val maxRunsScored = max(50f, max(runs1.toFloat(), runs2.toFloat()))
        val gridYCount = 5
        val rStep = maxRunsScored / gridYCount

        // Draw header labels
        drawContext.canvas.nativeCanvas.drawText("Runs scored", 15f, paddingTop - 40f, headerPaint)
        drawContext.canvas.nativeCanvas.drawText("Overs completed", paddingLeft + graphWidth / 2f, paddingTop + graphHeight + 52f, Paint().apply {
            color = MutedGrey.toArgb()
            textSize = 9.5f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        })

        // 1. Draw Background grid & labels of Y axis (Runs)
        for (i in 0..gridYCount) {
            val rVal = (rStep * i).toInt()
            val yOffset = paddingTop + graphHeight - (graphHeight * (rVal / maxRunsScored))
            // line
            drawLine(
                color = GlassBorder.copy(alpha = 0.15f),
                start = Offset(paddingLeft, yOffset),
                end = Offset(paddingLeft + graphWidth, yOffset),
                strokeWidth = 1f
            )
            // label
            drawContext.canvas.nativeCanvas.drawText(
                rVal.toString(),
                paddingLeft - 15f,
                yOffset + 3.5f * densityVal,
                textPaintRight
            )
        }

        // Vertical labels & grids of X axis (Overs)
        // To avoid crowded axis, draw every 5 overs if total > 10, otherwise every 1 or 2
        val overLabelStep = when {
            totalOvers <= 5 -> 1
            totalOvers <= 12 -> 2
            totalOvers <= 24 -> 5
            else -> 10
        }

        for (o in 0..totalOvers step overLabelStep) {
            val xOffset = paddingLeft + (stepX * o)
            drawLine(
                color = GlassBorder.copy(alpha = 0.15f),
                start = Offset(xOffset, paddingTop),
                end = Offset(xOffset, paddingTop + graphHeight),
                strokeWidth = 1f
            )
            // X-Axis labels
            drawContext.canvas.nativeCanvas.drawText(
                o.toString(),
                xOffset,
                paddingTop + graphHeight + 25f,
                textPaintCenter
            )
        }

        // Solid Axes
        drawLine(
            color = MutedGrey.copy(alpha = 0.4f),
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + graphHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = MutedGrey.copy(alpha = 0.4f),
            start = Offset(paddingLeft, paddingTop + graphHeight),
            end = Offset(paddingLeft + graphWidth, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // Helper mapper to canvas coordinates
        fun mapPoint(overFrac: Float, rVal: Float): Offset {
            val cx = paddingLeft + (overFrac / totalOvers) * graphWidth
            val cy = paddingTop + graphHeight - (rVal / maxRunsScored) * graphHeight
            return Offset(cx, cy)
        }

        // Draw Inning 1 line path (InfoTeal - smooth curves)
        if (trajectory1.isNotEmpty()) {
            val path1 = Path().apply {
                val start = mapPoint(0f, 0f)
                moveTo(start.x, start.y)
                trajectory1.forEach { p ->
                    val coord = mapPoint(p.first, p.second)
                    lineTo(coord.x, coord.y)
                }
            }
            drawPath(
                path = path1,
                color = InfoTeal,
                style = Stroke(width = 4f)
            )

            // draw wickets as small red circles
            wickets1.forEach { w ->
                val coord = mapPoint(w.first, w.second)
                drawCircle(
                    color = CleanWhite,
                    radius = 8f,
                    center = coord
                )
                drawCircle(
                    color = WicketCrimson,
                    radius = 5f,
                    center = coord
                )
            }
        }

        // Draw Inning 2 line path (GoldAccent)
        if (trajectory2.isNotEmpty()) {
            val path2 = Path().apply {
                val start = mapPoint(0f, 0f)
                moveTo(start.x, start.y)
                trajectory2.forEach { p ->
                    val coord = mapPoint(p.first, p.second)
                    lineTo(coord.x, coord.y)
                }
            }
            drawPath(
                path = path2,
                color = GoldAccent,
                style = Stroke(width = 4f)
            )

            // draw wickets for Inning 2
            wickets2.forEach { w ->
                val coord = mapPoint(w.first, w.second)
                drawCircle(
                    color = CleanWhite,
                    radius = 8f,
                    center = coord
                )
                drawCircle(
                    color = WicketCrimson,
                    radius = 5f,
                    center = coord
                )
            }
        }
    }
}

// Side-by-side Over scoring column comparison bar chart
@Composable
fun RunRateBarCanvas(
    deliveries: List<Delivery>,
    totalOvers: Int,
    teamAName: String,
    teamBName: String
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bar_chart_canvas")
    ) {
        val width = size.width
        val height = size.height

        val paddingLeft = 65f
        val paddingRight = 65f
        val paddingTop = 65f
        val paddingBottom = 65f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        // Draw gray background for the plot area
        drawRect(
            color = DarkBgSurface,
            topLeft = Offset(paddingLeft, paddingTop),
            size = Size(graphWidth, graphHeight)
        )

        val densityVal = this.density
        val textPaintRight = Paint().apply {
            color = CoolSlate.copy(alpha = 0.85f).toArgb()
            textSize = 9f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val textPaintCenter = Paint().apply {
            color = CoolSlate.copy(alpha = 0.85f).toArgb()
            textSize = 9f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = MutedGrey.toArgb()
            textSize = 9f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        // Group runs scored in each over for Innings 1 and 2
        val overRuns1 = IntArray(totalOvers) { 0 }
        val overRuns2 = IntArray(totalOvers) { 0 }

        deliveries.forEach { d ->
            val idx = d.overIndex
            if (idx >= 0 && idx < totalOvers) {
                val runs = d.runsBat + d.runsExtra
                if (d.innings == 1) {
                    overRuns1[idx] += runs
                } else if (d.innings == 2) {
                    overRuns2[idx] += runs
                }
            }
        }

        val maxRunsInAnyOver = max(12, max(overRuns1.maxOrNull() ?: 0, overRuns2.maxOrNull() ?: 0)).toFloat()

        // Draw header labels
        drawContext.canvas.nativeCanvas.drawText("Runs in over", 15f, paddingTop - 40f, headerPaint)
        drawContext.canvas.nativeCanvas.drawText("Over number", paddingLeft + graphWidth / 2f, paddingTop + graphHeight + 52f, Paint().apply {
            color = MutedGrey.toArgb()
            textSize = 9.5f * densityVal
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        })

        // Draw Horizontal background lines & labels
        val lineCount = 4
        for (i in 0..lineCount) {
            val fractionalY = i.toFloat() / lineCount.toFloat()
            val cy = paddingTop + graphHeight - (fractionalY * graphHeight)
            drawLine(
                color = GlassBorder.copy(alpha = 0.15f),
                start = Offset(paddingLeft, cy),
                end = Offset(paddingLeft + graphWidth, cy),
                strokeWidth = 1f
            )
            // Label
            val rVal = (fractionalY * maxRunsInAnyOver).toInt()
            drawContext.canvas.nativeCanvas.drawText(
                rVal.toString(),
                paddingLeft - 15f,
                cy + 3.5f * densityVal,
                textPaintRight
            )
        }

        // Draw Solid frame axes
        drawLine(
            color = MutedGrey.copy(alpha = 0.4f),
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + graphHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = MutedGrey.copy(alpha = 0.4f),
            start = Offset(paddingLeft, paddingTop + graphHeight),
            end = Offset(paddingLeft + graphWidth, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // Draw side-by-side columns
        val overCount = totalOvers
        val overSlotWidth = graphWidth / overCount.toFloat()
        val barGapPercent = 0.15f
        val barGroupWidth = overSlotWidth * (1f - barGapPercent)
        val singleBarWidth = barGroupWidth / 2f

        val overLabelStep = when {
            overCount <= 5 -> 1
            overCount <= 12 -> 2
            overCount <= 24 -> 5
            else -> 10
        }

        for (o in 0 until overCount) {
            val groupStartX = paddingLeft + (o * overSlotWidth) + (overSlotWidth * barGapPercent / 2f)
            val centerX = paddingLeft + (o * overSlotWidth) + (overSlotWidth / 2f)

            // Draw X axis label under slot
            if ((o + 1) % overLabelStep == 0 || o == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    (o + 1).toString(),
                    centerX,
                    paddingTop + graphHeight + 25f,
                    textPaintCenter
                )
            }

            // Bar 1 (Innings 1 - InfoTeal)
            val runs1 = overRuns1[o]
            if (runs1 > 0) {
                val bHeight1 = (runs1.toFloat() / maxRunsInAnyOver) * graphHeight
                val bx1 = groupStartX
                val by1 = paddingTop + graphHeight - bHeight1
                drawRect(
                    color = InfoTeal,
                    topLeft = Offset(bx1, by1),
                    size = Size(singleBarWidth - 2f, bHeight1)
                )
            }

            // Bar 2 (Innings 2 - GoldAccent)
            val runs2 = overRuns2[o]
            if (runs2 > 0) {
                val bHeight2 = (runs2.toFloat() / maxRunsInAnyOver) * graphHeight
                val bx2 = groupStartX + singleBarWidth
                val by2 = paddingTop + graphHeight - bHeight2
                drawRect(
                    color = GoldAccent,
                    topLeft = Offset(bx2, by2),
                    size = Size(singleBarWidth - 2f, bHeight2)
                )
            }
        }
    }
}
