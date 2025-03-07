package org.encalmo.utils

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.*
import org.fusesource.jansi.Ansi.Color.*
import org.fusesource.jansi.AnsiConsole
import sttp.client4.*
import sttp.model.Header
import sttp.model.StatusCode
import ujson.*

object HttpieFormatter {

  AnsiConsole.systemInstall()

  val httpHeadersNotToShow = Set("accept-encoding", "content-length")

  def showRequest[T](
      request: Request[T],
      ansi: Ansi,
      masked: Boolean = false
  ): String =
    val contentType = request.header("Content-Type").getOrElse("")
    request.body match {
      case StringBody(content, _, _) if contentType.contains("application/json") =>
        showHttpieCommand(request, ansi, masked)
        val json = JsonFormatter.parse(content)
        if (json != null && !json.isNull)
          ansi.fg(YELLOW).a(" \\\n  ").reset()
          showJsonAsCommandLineAttributes(json, None, ansi)

      case StringBody(content, _, _) =>
        ansi
          .fg(YELLOW)
          .a("echo '")
          .fg(BLUE)
          .a(content)
          .fg(YELLOW)
          .a("' | \\")
          .newline()
          .reset()
        showHttpieCommand(request, ansi, masked)

      case other =>
        showHttpieCommand(request, ansi, masked)
    }
    ansi.newline().newline()
    HttpFormatter.showRequest(request, ansi, masked)
    ansi.toString()

  private def showHttpieCommand[T](
      request: Request[T],
      ansi: Ansi,
      masked: Boolean
  ): Ansi =
    ansi
      .fgBrightYellow()
      .bold()
      .a("http")
      .boldOff()
      .fg(YELLOW)
      .a(" -v ")
      .a(request.method)
      .a(" ")
      .fgBrightCyan()
      .bold()
      .a(request.uri)
      .boldOff()
      .fg(YELLOW)

    val headers =
      request.headers
        .filterNot(h =>
          h.name.startsWith(":")
            || httpHeadersNotToShow.contains(h.name.toLowerCase())
        )
        .sortBy(_.name)
        .map(maskHeader(_, masked))
    if (headers.nonEmpty)
      headers.foreach { h =>
        ansi.fg(YELLOW).a(" \\\n  ")
        ansi
          .fg(BLUE)
          .a("'")
          .a(h.name)
          .a(":")
          .fgBrightBlue()
          .a(h.value)
          .fg(BLUE)
          .a("'")
      }
    ansi.reset()

  private val maskedHeaders: Set[String] = Set("X-API-KEY", "AUTHORIZATION", "X-Meridian-Api-Key", "X-API-Key")

  private def maskHeader(header: Header, shouldMask: Boolean): Header =
    if shouldMask && maskedHeaders.contains(header.name.toUpperCase())
    then Header(header.name, header.value.replaceAll(".", "X"))
    else header

  def showResponse[T](response: Response[T], ansi: Ansi): String =
    HttpFormatter.showResponse(response, ansi)

  private def colorOfResponse(statusCode: StatusCode): Color =
    if (statusCode.isSuccess) GREEN
    else if (statusCode.isClientError || statusCode.isServerError) RED
    else BLUE

  private def showJsonAsCommandLineAttributes(
      value: Value,
      nestedPath: Option[String] = None,
      ansi: Ansi,
      separator: String = " \\\n  "
  ): Ansi =
    value.match {
      case Str(v) => ansi.fg(BLUE).a("=").fg(CYAN).a(v).a("'").reset()
      case Num(v) =>
        ansi
          .fg(BLUE)
          .a(":=")
          .fg(MAGENTA)
          .a(JsonFormatter.numberFormattter.format(v))
          .a("'")
          .reset()
      case Bool(v) =>
        ansi.fg(BLUE).a(":=").fg(if (v) GREEN else RED).a(v).a("'").reset()
      case Null => ansi.fg(BLUE).a(":=").a("null").a("'").reset()

      case Arr(array) =>
        array.zipWithIndex.foreach { case (v, index) =>
          v match {
            case _: Obj | _: Arr =>
              showJsonAsCommandLineAttributes(
                v,
                nestedPath.map(_ + s"[$index]").orElse(Some(s"'[$index]")),
                ansi
              )
            case _ =>
              nestedPath.match {
                case Some(path) =>
                  ansi.fg(YELLOW).a(path).a("[").a(index).a("]")
                case None =>
                  ansi.fg(YELLOW).a("'").a("[").a(index).a("]")
              }
              showJsonAsCommandLineAttributes(v, nestedPath, ansi)
          }
          if (index < array.size - 1) ansi.fg(YELLOW).a(separator)
        }
        ansi

      case Obj(fields) =>
        fields.zipWithIndex.foreach { case ((name, v), index) =>
          v match {
            case _: Obj | _: Arr =>
              showJsonAsCommandLineAttributes(
                v,
                nestedPath.map(_ + s"[$name]").orElse(Some(s"'$name")),
                ansi
              )
            case _ =>
              nestedPath.match {
                case Some(path) =>
                  ansi.fg(YELLOW).a(path).a("[").a(name).a("]")
                case None =>
                  ansi.fg(YELLOW).a("'").a(name)
              }
              showJsonAsCommandLineAttributes(v, nestedPath, ansi)

          }
          if (index < fields.size - 1) ansi.fg(YELLOW).a(separator)
        }
        ansi
    }

}
