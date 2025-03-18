<a href="https://central.sonatype.com/artifact/org.encalmo/sttp-utils_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/sttp-utils_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/sttp-utils/scaladoc/org/encalmo/utils.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# sttp-utils

This Scala 3 library provides few extensions to the [sttp](https://github.com/softwaremill/sttp) library provided with [Scala toolkit](https://docs.scala-lang.org/toolkit/introduction.html).

## Dependencies

   - [Scala](https://www.scala-lang.org/) >= 3.3.5
   - [script-utils 0.9.1](https://central.sonatype.com/artifact/org.encalmo/script-utils_3)
   - [upickle-utils 0.9.9](https://central.sonatype.com/artifact/org.encalmo/upickle-utils_3)
   - [jansi 2.4.1](https://central.sonatype.com/artifact/org.fusesource.jansi/jansi)
   - [core ](https://central.sonatype.com/artifact/com.softwaremill.sttp.client4/core_3)
   - [upickle ](https://central.sonatype.com/artifact/com.softwaremill.sttp.client4/upickle_3)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "sttp-utils" % "0.9.3"

or with SCALA-CLI

    //> using dep org.encalmo::sttp-utils:0.9.3

## Examples

See: [Examples](https://github.com/encalmo/sttp-utils/blob/main/SttpUtils.test.scala)