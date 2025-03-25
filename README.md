<a href="https://github.com/encalmo/sttp-utils">![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)</a> <a href="https://central.sonatype.com/artifact/org.encalmo/sttp-utils_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/sttp-utils_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/sttp-utils/scaladoc/org/encalmo/utils.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# sttp-utils

This Scala 3 library provides few extensions to the [sttp](https://github.com/softwaremill/sttp) library provided with [Scala toolkit](https://docs.scala-lang.org/toolkit/introduction.html).

## Table of contents

- [Dependencies](#dependencies)
- [Usage](#usage)
- [Examples](#examples)

## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.3.5
   - org.encalmo [**script-utils** 0.9.1](https://central.sonatype.com/artifact/org.encalmo/script-utils_3) | [**upickle-utils** 0.9.9](https://central.sonatype.com/artifact/org.encalmo/upickle-utils_3)
   - org.fusesource.jansi [**jansi** 2.4.1](https://central.sonatype.com/artifact/org.fusesource.jansi/jansi)
   - com.softwaremill.sttp.client4 [**core** 4.0.0-RC1](https://github.com/softwaremill/sttp) | [**upickle** 4.0.0-RC1](https://github.com/softwaremill/sttp)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "sttp-utils" % "0.9.4"

or with SCALA-CLI

    //> using dep org.encalmo::sttp-utils:0.9.4

## Examples

See: [Examples](https://github.com/encalmo/sttp-utils/blob/main/SttpUtils.test.scala)

## Project content

```
├── .github
│   └── workflows
│       ├── pages.yaml
│       ├── release.yaml
│       └── test.yaml
│
├── .gitignore
├── .scalafmt.conf
├── HttpFormatter.scala
├── HttpieFormatter.scala
├── LICENSE
├── project.scala
├── README.md
├── RequestUtils.scala
├── SttpUtils.test.scala
└── test.sh
```