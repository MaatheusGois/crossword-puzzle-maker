package com.papauschek.ui

import scala.scalajs.js
import com.papauschek.puzzle.{Puzzle, PuzzleConfig, PuzzleWords}
import com.papauschek.ui.{Globals, HtmlRenderer}
import org.scalajs.dom
import org.scalajs.dom.Worker
import org.scalajs.dom.html.{Button, Div, Input, Select, TextArea}
import upickle.default.*
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExport
import scala.util.Random

/** the main user interface based on the `index.html` */
class MainPage:

  private var initialPuzzle: Puzzle = Puzzle.empty(PuzzleConfig())
  private var refinedPuzzle: Puzzle = initialPuzzle

  private var mainInputQuestions: Seq[String] = Nil
  private var mainInputWords: Seq[String] = Nil

  private val titleElement = dom.document.getElementById("title").asInstanceOf[Div]
  private val inputElement = dom.document.getElementById("input").asInstanceOf[Select]
  private val outputPuzzleElement = dom.document.getElementById("output-puzzle")
  private val outputCluesElement = dom.document.getElementById("output-clues")
  private val outputQuestionsElement = dom.document.getElementById("output-questions")
  
  private val resultInfoElement = dom.document.getElementById("result-info")

  private val generateButton = dom.document.getElementById("generate-button").asInstanceOf[Button]
  private val generateSpinner = dom.document.getElementById("generate-spinner").asInstanceOf[Div]

  private val resultWithoutElement = dom.document.getElementById("result-without").asInstanceOf[Input]
  private val resultPartialElement = dom.document.getElementById("result-partial").asInstanceOf[Input]
  private val resultFullElement = dom.document.getElementById("result-full").asInstanceOf[Input]

  private val widthInputElement = dom.document.getElementById("width").asInstanceOf[Input]
  private val heightInputElement = dom.document.getElementById("height").asInstanceOf[Input]

  private val languageSelect = dom.document.getElementById("language-select").asInstanceOf[Select]
  private val refineButton = dom.document.getElementById("refine-button").asInstanceOf[Button]
  private val printButton = dom.document.getElementById("print-button").asInstanceOf[Button]

  private val resultRow = dom.document.getElementById("result-row").asInstanceOf[Div]
  private val refineRow = dom.document.getElementById("refine-row").asInstanceOf[Div]
  private val cluesRow = dom.document.getElementById("clues-row").asInstanceOf[Div]

  generateButton.addEventListener("click", { _ => generateSolution() })
  refineButton.addEventListener("click", { _ => refineSolution() })
  printButton.addEventListener("click", { _ => printSolution() })

  resultWithoutElement.addEventListener("click", { _ => renderSolution() })
  resultPartialElement.addEventListener("click", { _ => renderSolution() })
  resultFullElement.addEventListener("click", { _ => renderSolution() })

  /** read the words from the user interface and generate the puzzle in the background using web workers */
  def generateSolution(): Unit =
    val newTitle = inputElement.options.toSeq(inputElement.selectedIndex).innerHTML
    titleElement.innerHTML = s"<h1>${newTitle}</h1>"

    val rawInputWords = js.Dynamic.global.BiblicalThemesPT.asInstanceOf[js.Dictionary[js.Array[String]]](inputElement.value).toSeq
    mainInputQuestions = rawInputWords
    val randomWords = rawInputWords.flatMap(selectRandomWord)
    val inputWords = randomWords.filter(word => word.nonEmpty && !word.startsWith("#"))

    if (inputWords.nonEmpty) {
      mainInputWords = PuzzleWords.sortByBest(inputWords)
      val puzzleConfig = PuzzleConfig(
        width = widthInputElement.valueAsNumber.toInt,
        height = heightInputElement.valueAsNumber.toInt
      )
      generateSpinner.classList.remove("invisible")
      generateButton.classList.add("invisible")

      PuzzleGenerator.send(NewPuzzleMessage(puzzleConfig, mainInputWords)).foreach {
        puzzles =>
          generateSpinner.classList.add("invisible")
          generateButton.classList.remove("invisible")
          resultRow.classList.remove("invisible")
          refineRow.classList.remove("invisible")
          cluesRow.classList.remove("invisible")
          initialPuzzle = puzzles.maxBy(_.density)
          refinedPuzzle = initialPuzzle
          renderSolution()
      }
    }


  /** show the generated puzzle */
  def renderSolution(): Unit =
    val showPartialSolution = resultPartialElement.checked
    val showFullSolution = resultFullElement.checked

    outputPuzzleElement.innerHTML = HtmlRenderer.renderPuzzle(
      refinedPuzzle,
      showSolution = showFullSolution,
      showPartialSolution = showPartialSolution)

    val unusedWords = mainInputWords.filterNot(refinedPuzzle.words.contains)
    val extraWords = refinedPuzzle.words -- initialPuzzle.words
    resultInfoElement.innerHTML = HtmlRenderer.renderPuzzleInfo(refinedPuzzle, unusedWords)
    outputCluesElement.innerHTML = HtmlRenderer.renderClues(refinedPuzzle, extraWords)
    outputQuestionsElement.innerHTML = HtmlRenderer.renderQuestions(mainInputQuestions, refinedPuzzle, extraWords)

  /** add words from a chosen dictionary to the puzzle */
  def refineSolution(): Unit =
    val language = languageSelect.value
    val words = Globals.window(language).filter(_.length >= 4)
    refinedPuzzle = Puzzle.finalize(initialPuzzle, words.toList)
    renderSolution()

  /** show the print dialog */
  def printSolution(): Unit =
    dom.window.print()

  /** select random and normalize words */
  private def selectRandomWord(sentence: String): Option[String] = {
    val words = sentence.replaceAll("\\(.*?\\)", "").split("\\s+")
    val cleanedWords = words.map(_.replaceAll("[.,;]", "").toUpperCase)
    val filteredWords = cleanedWords.filter(_.length > 4)
    if (filteredWords.nonEmpty) Some(filteredWords(Random.nextInt(filteredWords.length))) else None
  }

