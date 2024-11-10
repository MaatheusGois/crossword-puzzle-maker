package com.crosswordmaker.ui

import scala.scalajs.js
import com.crosswordmaker.puzzle.{Puzzle, PuzzleConfig, PuzzleWords}
import com.crosswordmaker.ui.{Globals, HtmlRenderer}
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
  private val inputThemeElement = dom.document.getElementById("input").asInstanceOf[Select]
  private val inputVersionElement = dom.document.getElementById("input-version").asInstanceOf[Select]

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
  private val heighInputElement = dom.document.getElementById("height").asInstanceOf[Input]

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

  def readJsonFileSync(filePath: String): String = {
    val xhr = new dom.XMLHttpRequest()

    // Define que a requisição será síncrona
    xhr.open("GET", filePath, false) // o "false" aqui significa síncrono

    try {
      xhr.send()
      if (xhr.status == 200) {
        xhr.responseText // Retorna o texto da resposta
      } else {
        throw new Exception(s"Erro ao carregar o arquivo: ${xhr.statusText}")
      }
    } catch {
      case e: Exception =>
        throw new Exception(s"Erro na requisição: ${e.getMessage}")
    }
  }

  def getBiblicalThemeWords(theme: String, filePath: String): Seq[String] = {
    // Ler o arquivo JSON de forma síncrona
    val rawInputWordsTest = readJsonFileSync(filePath)

    // Usar upickle para parsear o JSON em um objeto Scala
    val parsedJson = upickle.default.read[Map[String, Seq[String]]](rawInputWordsTest)

    // Obter os dados do tema baseado no valor de inputThemeElement
    parsedJson.get(theme) match {
      case Some(words) =>
        // Se o tema for encontrado, retorna as palavras (frases) associadas a ele
        words
      case None =>
        // Caso o tema não exista, retorna uma sequência vazia
        Seq.empty
    }
  }

  /** read the words from the user interface and generate the puzzle in the background using web workers */
  def generateSolution(): Unit =
    val newTitle = inputThemeElement.options.toSeq(inputThemeElement.selectedIndex).innerHTML
    titleElement.innerHTML = s"<h1>${newTitle}</h1>"

    val theme = inputThemeElement.value
    val version = inputVersionElement.value
    val filePath = s"./data/croisee/versicles-$version.json"

    val rawInputWords = getBiblicalThemeWords(theme, filePath)
    println(s"rawInputWords: $rawInputWords")

    mainInputQuestions = rawInputWords
    val randomWords = rawInputWords.flatMap(selectRandomWord)
    val inputWords = randomWords.filter(word => word.nonEmpty && !word.startsWith("#"))

    if (inputWords.nonEmpty) {
      mainInputWords = PuzzleWords.sortByBest(inputWords)
      val puzzleConfig = PuzzleConfig(
        width = widthInputElement.valueAsNumber.toInt,
        height = heighInputElement.valueAsNumber.toInt
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

