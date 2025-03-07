package org.encalmo.utils

import org.fusesource.jansi.Ansi
import sttp.client4.*
import sttp.client4.quick

import scala.io.AnsiColor

object RequestUtils {

  lazy val debugHttp: Boolean =
    System.getenv().getOrDefault("HTTP_DEBUG_MODE", "ON").match {
      case "ON"     => true
      case "MASKED" => true
      case _        => false
    }

  lazy val maskHttp: Boolean =
    System.getenv().getOrDefault("HTTP_DEBUG_MODE", "ON").match {
      case "ON"     => false
      case "MASKED" => true
      case _        => false
    }

  lazy val debugHttpie: Boolean =
    System.getenv().getOrDefault("HTTPIE_DEBUG_MODE", "ON").match {
      case "ON"     => true
      case "MASKED" => true
      case _        => false
    }

  lazy val maskHttpie: Boolean =
    System.getenv().getOrDefault("HTTPIE_DEBUG_MODE", "ON").match {
      case "ON"     => false
      case "MASKED" => true
      case _        => false
    }

  lazy val hideAnsiColors: Boolean =
    Option(System.getenv("NO_COLOR")).contains("1")
      || Option(System.getenv("ANSI_COLORS_MODE")).contains("OFF")

  private def getAnsi(): Ansi =
    Ansi.setEnabled(!hideAnsiColors)
    Ansi.ansi()

  extension [T](request: Request[T]) {
    final inline def send(comment: String): Response[T] =
      sendRequest(comment, debugHttp, debugHttpie, maskHttp | maskHttpie)

    final inline def sendRequest(comment: String, debug: Boolean): Response[T] =
      sendRequest(comment, debug, debug, false)

    final inline def sendRequest(
        comment: String,
        debugHttp: Boolean = true,
        debugHttpie: Boolean = true,
        mask: Boolean = true
    ): Response[T] =
      if (debugHttpie || debugHttp) then
        ConsoleUtils.printlnMessageBoxed(
          color = AnsiColor.WHITE,
          frame = '-',
          message = comment
        )
      if (debugHttpie) then
        println(
          HttpieFormatter.showRequest(
            request,
            masked = mask,
            ansi = getAnsi()
          )
        )
      else if (debugHttp) then
        println(
          HttpFormatter.showRequest(
            request,
            masked = mask,
            ansi = getAnsi()
          )
        )
      NoRetry.send(request, debugHttp | debugHttpie)

    final inline def sendWithRetry(retry: Retry, comment: String): Response[T] =
      if (debugHttpie || debugHttp) then
        ConsoleUtils.printlnMessageBoxed(
          color = AnsiColor.WHITE,
          frame = '-',
          message = comment
        )
      if (debugHttpie) then
        println(
          HttpieFormatter
            .showRequest(request, masked = maskHttpie, ansi = getAnsi())
        )
      else if (debugHttp) then
        println(
          HttpFormatter.showRequest(
            request,
            masked = maskHttp,
            ansi = getAnsi()
          )
        )
      retry.send(request, debugHttp | debugHttpie)
  }

  trait Retry {
    def send[T](request: Request[T], debugResponse: Boolean): Response[T]
  }

  object NoRetry extends Retry {
    override def send[T](
        request: Request[T],
        debugResponse: Boolean
    ): Response[T] =
      val response = request.send(quick.backend)
      if (debugResponse) then println(HttpFormatter.showResponse(response, ansi = getAnsi()))
      response
  }

  trait RetryPolicy[T] {
    def send(
        request: Request[T],
        shouldRetry: Response[T] => Boolean,
        debugResponse: Boolean
    ): Response[T]
  }

  final class DelayedRetryPolicy[T](
      initialResponse: Response[T],
      remainingAttempts: Int,
      nextDelay: Long,
      backoffFactor: Double,
      backoffDeclineFactor: Double
  ) extends RetryPolicy[T] {

    override def send(
        request: Request[T],
        shouldRetry: Response[T] => Boolean,
        debugResponse: Boolean
    ): Response[T] =
      if (remainingAttempts <= 0)
      then {
        println(
          s"${AnsiColor.RED_B}${AnsiColor.WHITE} Retrying HTTP request did not succeed, returning an original response ${AnsiColor.RESET}"
        )
        initialResponse
      } else {
        println(
          s"${AnsiColor.YELLOW_B}${AnsiColor.BLACK} About to retry HTTP request after ${nextDelay} ms ... ${AnsiColor.RESET}"
        )
        Thread.sleep(nextDelay)
        val response = NoRetry.send(request, debugResponse)
        if shouldRetry(response)
        then nextPolicy.send(request, shouldRetry, debugResponse)
        else response
      }

    private inline def nextPolicy: RetryPolicy[T] = new DelayedRetryPolicy[T](
      initialResponse,
      remainingAttempts - 1,
      (nextDelay * backoffFactor).toLong,
      backoffFactor * backoffDeclineFactor,
      backoffDeclineFactor
    )
  }

  object DelayedRetryPolicy {

    def apply(
        remainingAttempts: Int = 4,
        nextDelay: Long = 1000,
        backoffFactor: Double = Math.sqrt(2),
        backoffDeclineFactor: Double = 1.0d
    ) =
      [T] =>
        (initialResponse: Response[T]) =>
          new DelayedRetryPolicy(
            initialResponse = initialResponse,
            remainingAttempts = remainingAttempts,
            nextDelay = nextDelay,
            backoffFactor = backoffFactor,
            backoffDeclineFactor = backoffDeclineFactor
        )

  }

  trait RetryWithPolicy extends Retry {
    self =>

    def initialPolicy[T](initialResponse: Response[T]): RetryPolicy[T]
    def shouldRetry[T](response: Response[T]): Boolean

    override def send[T](
        request: Request[T],
        debugResponse: Boolean
    ): Response[T] =
      val firstResponse = NoRetry.send(request, debugResponse)
      if shouldRetry(firstResponse)
      then initialPolicy(firstResponse).send(request, shouldRetry, debugResponse)
      else firstResponse

    def withInitialPolicy(policy: [T] => Response[T] => RetryPolicy[T]): RetryWithPolicy =
      new RetryWithPolicy {
        def initialPolicy[T](initialResponse: Response[T]): RetryPolicy[T] = policy[T](initialResponse)
        def shouldRetry[T](response: Response[T]): Boolean = self.shouldRetry[T](response)
      }

    def orWhen(condition: [T] => Response[T] => Boolean): RetryWithPolicy =
      new RetryWithPolicy {
        def initialPolicy[T](initialResponse: Response[T]): RetryPolicy[T] = self.initialPolicy(initialResponse)
        def shouldRetry[T](response: Response[T]): Boolean =
          self.shouldRetry[T](response) || condition(response)
      }

    def andWhen(condition: [T] => Response[T] => Boolean): RetryWithPolicy =
      new RetryWithPolicy {
        def initialPolicy[T](initialResponse: Response[T]): RetryPolicy[T] = self.initialPolicy(initialResponse)
        def shouldRetry[T](response: Response[T]): Boolean =
          self.shouldRetry[T](response) && condition(response)
      }

  }

  object RetryOn5xx extends RetryWithPolicy {

    def initialPolicy[T](initialResponse: Response[T]): RetryPolicy[T] =
      new DelayedRetryPolicy(
        initialResponse = initialResponse,
        remainingAttempts = 4,
        nextDelay = 1000,
        backoffFactor = Math.sqrt(2),
        backoffDeclineFactor = 1.0d
      )

    def shouldRetry[T](response: Response[T]): Boolean =
      response.code.isServerError
  }

  class RetryWithDelay(
      maxAttempts: Int,
      initialDelay: Long = 1000,
      backoffFactor: Double = 2,
      backoffDeclineFactor: Double = 1.0d,
      shouldRetryFx: [T] => Response[T] => Boolean
  ) extends RetryWithPolicy {

    def initialPolicy[T](initialResponse: Response[T]): RetryPolicy[T] =
      new DelayedRetryPolicy(
        initialResponse,
        maxAttempts - 1,
        initialDelay,
        backoffFactor,
        backoffDeclineFactor
      )

    def shouldRetry[T](response: Response[T]): Boolean =
      shouldRetryFx(response)
  }

  object Retry {

    def apply(
        maxAttempts: Int,
        initialDelay: Long = 1000,
        backoffFactor: Double = 2,
        backoffDeclineFactor: Double = 1.0d
    )(shouldRetryFx: [T] => Response[T] => Boolean): Retry =
      new RetryWithDelay(
        maxAttempts,
        initialDelay,
        backoffFactor,
        backoffDeclineFactor,
        shouldRetryFx
      )
  }

}
