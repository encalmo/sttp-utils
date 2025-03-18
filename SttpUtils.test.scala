package org.encalmo.utils

import sttp.client4.*
import sttp.model.*
import org.fusesource.jansi.Ansi

class SttpUtilsSpec extends munit.FunSuite {

  test("SttpUtils should format http request and response") {

    val request = basicRequest
      .response(asStringAlways)
      .get(Uri.unsafeParse("https://ukr7depy1b.execute-api.eu-central-1.amazonaws.com/live/health"))
      .contentType(MediaType.ApplicationJson)
      .header("X-Api-Key", "7q78w75skajn8731§wqhwjhqe")
      .body("""{"foo":"bar", "price": 1234, "is_ready":false, "is_available":true}""")

    val output1 = HttpFormatter.showRequest(request, Ansi.ansi(), masked = true)
    println(output1)

    val output2 = HttpieFormatter.showRequest(request, Ansi.ansi(), masked = true)
    println(output2)

    val response = request.send(quick.backend)

    val output3 = HttpFormatter.showResponse(response, Ansi.ansi())
    println(output3)
  }

  test("SttpUtils should format http request and response") {

    val request = basicRequest
      .response(asStringAlways)
      .get(
        Uri.unsafeParse(
          "https://index.scala-lang.org/api/v1/artifacts/com.softwaremill.sttp.client4/upickle_3/4.0.0-RC1"
        )
      )
      .acceptEncoding("application/json")

    val output1 = HttpFormatter.showRequest(request, Ansi.ansi(), masked = true)
    println(output1)

    val response = request.send(quick.backend)

    val output3 = HttpFormatter.showResponse(response, Ansi.ansi())
    println(output3)
  }

}
