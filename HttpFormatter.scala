package org.encalmo.utils

import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.*
import org.fusesource.jansi.Ansi.Color.*
import sttp.client4.*
import sttp.model.StatusCode
import ujson.*
import sttp.model.Header

object HttpFormatter {

  AnsiConsole.systemInstall()

  lazy val hideAnsiColors: Boolean =
    Option(System.getenv("NO_COLOR")).contains("1")
      || Option(System.getenv("ANSI_COLORS_MODE")).contains("OFF")

  def showRequest[T](
      request: Request[T],
      ansi: Ansi,
      masked: Boolean = false
  ): String =
    request.body match {
      case StringBody(content, _, _) =>
        showHttpRequest(request, ansi, masked)
        format(request.contentType, request.body, ansi)
        ansi
          .newline()
          .reset()
          .toString()

      case other =>
        showHttpRequest(request, ansi, masked)
    }
    ansi.toString()

  def showResponse[T](response: Response[T], ansi: Ansi): String =
    ansi
      .a("  ")
      .fg(colorOfResponse(response.code))
      .a("HTTP/1.1 ")
      .bold()
      .a(response.code)
      .a(" ")
      .a(response.statusText)
      .boldOff()
      .a(" ")
      .a(response.statusText)
      .newline()
    response.headers
      .filterNot(_.name.startsWith(":"))
      .sortBy(_.name)
      .foreach { h =>
        ansi
          .a("  ")
          .fg(BLUE)
          .a(h.name)
          .a(": ")
          .fgBrightBlue()
          .a(h.value)
          .newline()
      }
    ansi.a("  ")
    format(response.contentType, response.body, ansi)
    ansi
      .newline()
      .reset()
      .toString()

  private def showHttpRequest[T](
      request: Request[T],
      ansi: Ansi,
      masked: Boolean
  ): Ansi =
    ansi
      .a("  ")
      .fgBrightYellow()
      .bold()
      .a(request.method)
      .boldOff()
      .a(" ")
      .fgBrightCyan()
      .bold()
      .a(request.uri)
      .boldOff()
      .fg(YELLOW)
      .newline()
    request.headers
      .filterNot(_.name.startsWith(":"))
      .sortBy(_.name)
      .map(maskHeader(_, masked))
      .foreach { h =>
        ansi
          .a("  ")
          .fg(BLUE)
          .a(h.name)
          .a(": ")
          .fgBrightBlue()
          .a(h.value)
          .newline()
      }
    ansi.a("  ")

  private val maskedHeaders: Set[String] = Set("X-API-KEY", "AUTHORIZATION", "X-Meridian-Api-Key", "X-API-Key")

  private def maskHeader(header: Header, shouldMask: Boolean): Header =
    if shouldMask && maskedHeaders.contains(header.name.toUpperCase())
    then Header(header.name, header.value.replaceAll(".", "X"))
    else header

  private def format[T](
      contentType: Option[String],
      body: T,
      ansi: Ansi
  ): Ansi =
    (contentType, Option(body)) match {
      case (Some(ct), Some(json: ujson.Value)) if ct.contains("application/json") =>
        if (json != Null)
        then
          if (hideAnsiColors)
          then ansi.a(ujson.write(json))
          else
            JsonFormatter.prettyPrintWithAnsiColors(
              json,
              indentLevel = 1,
              ansi = ansi
            )
        else ansi
      case (Some(ct), Some(StringBody(content, _, _))) if ct.contains("application/json") =>
        val json = JsonFormatter.parse(content)
        if (json != Null)
        then
          if (hideAnsiColors)
          then ansi.a(ujson.write(json))
          else
            JsonFormatter
              .prettyPrintWithAnsiColors(json, indentLevel = 1, ansi = ansi)
        else ansi
      case (ct, Some(StringBody(content, encoding, contentType))) if content != null =>
        ansi.a(content)
      case (ct, Some(other)) if other != null =>
        ansi.a(other.toString())
      case _ =>
        ansi
    }

  private def colorOfResponse(statusCode: StatusCode): Color =
    if (statusCode.isSuccess) GREEN
    else if (statusCode.isClientError || statusCode.isServerError) RED
    else BLUE

}
