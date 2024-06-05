package com.crosswordmaker.ui

import com.crosswordmaker.puzzle.{Point, Puzzle}
import org.scalajs.dom
import scala.scalajs.js
import org.scalajs.dom.html.{Select}
import scala.collection.immutable.Seq
import scala.util.matching.Regex

object HtmlRenderer:

  /** @return HTML for rendering a puzzle
   * @param puzzle the puzzle to render
   * @param showSolution if true, shows the solution (all characters) of the puzzle
   * @param showPartialSolution if true, shows some randomly selected solution characters of the puzzle (but not all of them) */
  def renderPuzzle(puzzle: Puzzle,
                   showSolution: Boolean = false,
                   showPartialSolution: Boolean = false): String =

    val annotation = puzzle.getAnnotation

    val partialPoints = puzzle.getCharsShownInPartialSolution()

    def renderCell(x: Int, y: Int): String =
      puzzle.getChar(x, y) match {
        case ' ' => ""
        case char =>

          val showLetter = showSolution || (showPartialSolution && partialPoints.contains(Point(x, y)))
          val svgLetter = Option.when(showLetter) {
            s"""<text x="${x * 10 + 4.5}" y="${(y + 1) * 10 - 2}" text-anchor="middle" class="letter">$char</text>"""
          }

          val svgAnnotation = annotation.get(Point(x, y)) match {
            case Some(anno) if anno.nonEmpty =>
              val annotationIndices = anno.map(_.index).mkString(",")
              Some(s"""<text x="${x * 10 + 0.8}" y="${(y + 1) * 10 - 0.8}" class="annotation">$annotationIndices</text>""")
            case _ => None
          }

          val svgCell = s"""<rect x="${x * 10}" y="${y * 10}" rx="0.5" ry="0.5" width="10" height="10"
            |  style="fill:white;stroke:black;stroke-width:0.3" />""".stripMargin

          svgCell + svgAnnotation.mkString + svgLetter.mkString
      }

    def renderHeight(y: Int): String =
      (0 until puzzle.config.width).map(renderCell(_, y)).mkString("\r\n")

    val renderedPuzzle = (0 until puzzle.config.height).map(renderHeight).mkString("\r\n")

    s"""<svg viewBox="-1 -1 ${puzzle.config.width * 10 + 2} ${puzzle.config.height * 10 + 2}">
      |  <style>
      |    .annotation {
      |      font: 3px sans-serif;
      |      fill: #999999;
      |    }
      |    .letter {
      |      font: 8px sans-serif;
      |      fill: black;
      |    }
      |  </style>
      |  $renderedPuzzle
      |</svg>""".stripMargin

  def renderQuestions(questions: Seq[String], puzzle: Puzzle, extraWords: Set[String]): String = {
    val annotation = puzzle.getAnnotation
    val sortedAnnotationValues = annotation.values.flatten.toSeq.sortBy(_.index)

    // Conjunto para armazenar as palavras já usadas em pistas
    var usedWords = Set.empty[String]

    def renderDescriptions(vertical: Boolean): String = {
      sortedAnnotationValues.filter(_.vertical == vertical).map {
        p =>
          val word = p.word
          // Verifica se a palavra já foi usada em alguma pista já processada
          if (!usedWords.exists(_.toUpperCase.contains(word.toUpperCase))) {
            def replaceWord(str: String): String = {
              str.toUpperCase.replace(word.toUpperCase, "_____" * word.length)
            }

            val regex = "\\(.*?\\)".r
            val firstQuestionFinded: String = questions
              .find(_.toUpperCase.contains(word.toUpperCase))
              .map(replaceWord)
              .getOrElse("")
              .replace("(", "<strong>(")
              .replace(")", ")</strong>")

            // Adiciona a palavra ao conjunto de palavras usadas
            usedWords += word.toUpperCase

            // Retorna a descrição da pista apenas se a palavra não foi usada em outra pista
            if (!firstQuestionFinded.isEmpty) {
              s"""<div  class="mb-4">${p.index}) $firstQuestionFinded</div>"""
            } else ""
          } else ""
      }.mkString("\r\n")
    }

    s"""<div class="row">
      |  <div class="col-lg-6">
      |    <h4>Horizontal</h4>
      |    <p>${renderDescriptions(vertical = false)}</p>
      |  </div>
      |  <div class="col-lg-6">
      |    <h4>Vertical</h4>
      |    <p>${renderDescriptions(vertical = true)}</p>
      |  </div>
      |</div>
      |""".stripMargin
  }



  /** @return HTML representing the clues (= solution) words for this puzzle */
  def renderClues(puzzle: Puzzle, extraWords: Set[String]): String =

    val annotation = puzzle.getAnnotation
    val sortedAnnotationValues = annotation.values.flatten.toSeq.sortBy(_.index)

    def renderDescriptions(vertical: Boolean): String = {
      sortedAnnotationValues.filter(_.vertical == vertical).map {
        p =>
          val formattedWord = if (extraWords.contains(p.word)) s"<strong>${p.word}</strong>" else p.word
          "<div>" + p.index + ") " + formattedWord + "</div>"
      }.mkString("\r\n")
    }

    s"""<div class="row">
       |  <div class="col-lg-6">
       |    <h4>Horizontal</h4>
       |    <p>${renderDescriptions(vertical = false)}</p>
       |  </div>
       |  <div class="col-lg-6">
       |    <h4>Vertical</h4>
       |    <p>${renderDescriptions(vertical = true)}</p>
       |  </div>
       |</div>
       |""".stripMargin


  /** @return HTML representing some additional info about the puzzle, such as density and discarded words. */
  def renderPuzzleInfo(puzzle: Puzzle, unusedWords: Seq[String]): String =
    val infoText = s"Este quebra-cabeça tem uma <strong>densidade de ${(puzzle.density * 100).round}%</strong>. "
    val unusedInfoText = Option.when(unusedWords.nonEmpty)(s"As seguintes palavras da sua lista NÃO foram utilizadas: ${unusedWords.mkString(", ")}").mkString
    infoText + unusedInfoText

