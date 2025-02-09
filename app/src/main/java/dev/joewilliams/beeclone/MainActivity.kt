@file:OptIn(ExperimentalLayoutApi::class)

package dev.joewilliams.beeclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import dev.joewilliams.beeclone.model.HexTile
import dev.joewilliams.beeclone.ui.theme.BeeCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = MainViewModel(LocalContext.current.resources)
            val words by viewModel.enteredWordsState.collectAsState()
            val enteredWord by viewModel.enteredLettersState.collectAsState()
            val tiles by viewModel.combTiles.collectAsState()
            val score by viewModel.scoreState.collectAsState()
            BeeCloneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        WordList(words = words)
                        Text("Score: $score")
                        EnteredWord(word = enteredWord)
                        if (tiles.size == 7) {
                            Honeycomb(letters = tiles) {
                                viewModel.tileTapped(it)
                            }
                        }
                        GameButtons(
                            onShuffleTapped = { viewModel.shuffleTapped() },
                            onNewGameTapped = { viewModel.newGameTapped() },
                            onBackTapped = { viewModel.backTapped() },
                            onClearTapped = { viewModel.clearTapped() },
                            onEnterTapped = { viewModel.enterTapped() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordList(
    modifier: Modifier = Modifier,
    words: List<String>
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (word in words) {
            Text(word)
        }
    }
}

@Composable
fun EnteredWord(
    modifier: Modifier = Modifier,
    word: String
) {
    Text(modifier = modifier, text = word)
}

@Composable
fun Honeycomb(
    modifier: Modifier = Modifier,
    letters: List<HexTile>,
    onLetterTapped: (HexTile) -> Unit
) {
    if (letters.count() < 7 || !letters.any { it.isCenter }) return
    val center = letters.first { it.isCenter }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((HEX_SIZE / 6).dp)) {
        Column {
            Box(modifier = Modifier.size((HEX_SIZE / 4).dp))
            CombTile(tile = letters.filter { !it.isCenter }[0], onTapped = onLetterTapped)
            CombTile(tile = letters.filter { !it.isCenter }[1], onTapped = onLetterTapped)
        }
        Column {
            CombTile(tile = letters.filter { !it.isCenter }[2], onTapped = onLetterTapped)
            CombTile(tile = center, onTapped = onLetterTapped)
            CombTile(tile = letters.filter { !it.isCenter }[3], onTapped = onLetterTapped)
        }
        Column {
            Box(modifier = Modifier.size((HEX_SIZE / 4).dp))
            CombTile(tile = letters.filter { !it.isCenter }[4], onTapped = onLetterTapped)
            CombTile(tile = letters.filter { !it.isCenter }[5], onTapped = onLetterTapped)
        }
    }
}

@Composable
fun CombTile(
    modifier: Modifier = Modifier,
    tile: HexTile,
    onTapped: (HexTile) -> Unit
) {
    Box(modifier = modifier
        .size((HEX_SIZE / 2).dp)
        .clickable { onTapped.invoke(tile) }
        .drawWithCache {
            val roundedPolygon = RoundedPolygon(
                numVertices = 6,
                radius = HEX_SIZE,
                centerX = HEX_SIZE / 2,
                centerY = HEX_SIZE / 2
            )
            val roundedPolygonPath = roundedPolygon
                .toPath()
                .asComposePath()
            onDrawBehind {
                drawPath(
                    roundedPolygonPath,
                    color = if (tile.isCenter) Color.Yellow else Color.Gray
                )
            }
        }) {
        Text(modifier = Modifier.align(Alignment.Center), text = tile.letter.toString())
    }
}

const val HEX_SIZE = 120f

@Composable
fun GameButtons(
    modifier: Modifier = Modifier,
    onEnterTapped: () -> Unit,
    onShuffleTapped: () -> Unit,
    onBackTapped: () -> Unit,
    onClearTapped: () -> Unit,
    onNewGameTapped: () -> Unit
) {
    FlowRow(modifier = modifier) {
        Button(onClick = onEnterTapped) {
            Text("Enter")
        }
        Button(onClick = onShuffleTapped) {
            Text("Shuffle")
        }
        Button(onClick = onBackTapped) {
            Text("Back")
        }
        Button(onClick = onClearTapped) {
            Text("Clear")
        }
        Button(onClick = onNewGameTapped) {
            Text("New Game")
        }
    }
}