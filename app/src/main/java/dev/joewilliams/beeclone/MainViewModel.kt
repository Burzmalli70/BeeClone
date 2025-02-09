package dev.joewilliams.beeclone

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.joewilliams.beeclone.model.HexTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel(private val resources: Resources): ViewModel() {
    private val dictionary: MutableMap<Char,MutableMap<Char,MutableMap<Char,MutableList<String>>>> = mutableMapOf()
    private val mutableCombTiles: MutableStateFlow<List<HexTile>> = MutableStateFlow(emptyList())
    val combTiles: StateFlow<List<HexTile>> = mutableCombTiles
    private val mutableEnteredLetters: MutableStateFlow<String> = MutableStateFlow("")
    val enteredLettersState: StateFlow<String> = mutableEnteredLetters
    private val mutableEnteredWords: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val enteredWordsState: StateFlow<List<String>> = mutableEnteredWords
    private val mutableScore: MutableStateFlow<Int> = MutableStateFlow(0)
    val scoreState: StateFlow<Int> = mutableScore
    private val mutableInvalidWordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val invalidWordState: StateFlow<Boolean> = mutableInvalidWordState
    private val possiblePangrams: MutableList<String> = arrayListOf()
    private var centerLetter: Char = ' '

    private var ready = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadDictionary()
            populateCombTiles()
        }
    }

    fun tileTapped(tile: HexTile) {
        viewModelScope.launch {
            mutableEnteredLetters.emit(mutableEnteredLetters.value + tile.letter)
        }
    }

    fun backTapped() {
        if (mutableEnteredLetters.value.isEmpty()) return
        viewModelScope.launch {
            mutableEnteredLetters.emit(mutableEnteredLetters.value.subSequence(0, mutableEnteredLetters.value.length - 1).toString())
        }
    }

    fun shuffleTapped() {

    }

    fun clearTapped() {
        viewModelScope.launch {
            mutableEnteredLetters.emit("")
        }
    }

    fun enterTapped() {
        scoreWord()
    }

    fun newGameTapped() {
        viewModelScope.launch {
            mutableScore.emit(0)
            mutableEnteredWords.emit(emptyList())
            mutableInvalidWordState.emit(false)
            mutableEnteredLetters.emit("")
            populateCombTiles()
        }
    }

    private fun resetInvalid() {
        viewModelScope.launch {
            mutableInvalidWordState.emit(false)
        }
    }

    private fun getPangram(): String {
        return possiblePangrams.random()
    }

    private fun populateCombTiles() {
        viewModelScope.launch {
            val pangram = getPangram()

            val letterArray = ArrayList<Char>()

            for (c in pangram) {
                if (!letterArray.contains(c)) {
                    letterArray.add(c)
                }
            }

            mutableCombTiles.emit(letterArray.mapIndexed { index, c ->
                HexTile(letter = c, isCenter = index == 0)
            })

            centerLetter = mutableCombTiles.value.first { it.isCenter }.letter
        }
    }

    private fun scoreWord() {
        var score = if (isPangram()) 7 else 0
        if (!isWordValid()) {
            viewModelScope.launch {
                mutableInvalidWordState.emit(true)
                delay(500)
                mutableInvalidWordState.emit(false)
            }
        } else {
            viewModelScope.launch {
                mutableEnteredWords.emit(enteredWordsState.value.plus(enteredLettersState.value))
                score += if (mutableEnteredLetters.value.length == 4) 1 else mutableEnteredLetters.value.length
                mutableScore.emit(mutableScore.value + score)
                mutableEnteredLetters.emit("")
            }
        }
    }

    private fun isPotentialPangram(word: String): Boolean {
        if (word.length < 7) return false
        val letterMap: MutableMap<Char,Int> = mutableMapOf()
        for(c in word) {
            letterMap[c] = 1
            if (letterMap.size > 7) return false
        }
        return letterMap.size == 7
    }

    private fun isPangram(): Boolean {
        for (tile in mutableCombTiles.value) {
            if (!mutableEnteredLetters.value.contains(tile.letter)) return false
        }

        return true
    }

    private fun isWordValid(): Boolean {
        val word = mutableEnteredLetters.value
        if (word.length < 4) return false
        if (!word.contains(centerLetter)) return false
        if (enteredWordsState.value.contains(word)) return false
        return dictionary[word[0]]?.get(word[1])?.get(word[2])?.contains(word) ?: false
    }

    private fun loadDictionary() {
        val istream = resources.openRawResource(R.raw.dictionary)
        val reader = BufferedReader(InputStreamReader(istream))
        var word: String? = ""
        var lastStartLetter = 'A'
        print("Now loading $lastStartLetter")
        do {
            word = reader.readLine()
            if (word == null || word.length < 4 || word.lowercase().contains('s')) continue
            if (isPotentialPangram(word)) possiblePangrams.add(word)
            if (word[0] != lastStartLetter) {
                lastStartLetter = word[0]
                print("Now loading ${word[0]}")
            }
            val subdictionary = dictionary[word[0]] ?: mutableMapOf()
            val sublist = subdictionary[word[1]] ?: mutableMapOf()
            val subsublist = sublist[word[2]] ?: mutableListOf()
            subsublist.add(word)
            sublist[word[2]] = subsublist
            subdictionary[word[1]] = sublist
            dictionary[word[0]] = subdictionary
        } while (word != null)
        istream.close()
        ready = true
    }
}