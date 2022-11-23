name := "magic_list_maker-server"

version := "1.0"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, SbtWeb)

scalaVersion := "2.11.8"

libraryDependencies += javaJdbc
libraryDependencies += ehcache
libraryDependencies += guice
libraryDependencies += javaWs
libraryDependencies += filters
libraryDependencies += specs2 % Test
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.1"
libraryDependencies += "com.stripe" % "stripe-java" % "5.6.0"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.8"
libraryDependencies += "com.myjeeva.poi" % "excelReader" % "1.2"
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.0"
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212.jre7"
libraryDependencies += "com.google.cloud" % "google-cloud-storage" % "1.0.0"
libraryDependencies += "net.spy" % "spymemcached" % "2.12.3"

unmanagedResourceDirectories in Test += baseDirectory(_ / "target/web/public/test").value

pipelineStages := Seq(rjs, digest, gzip)

DigestKeys.indexPath := Some("javascripts/versioned.js")
WebKeys.packagePrefix in Assets := "public/"

includeFilter in gzip := "*.html" || "*.css" || "*.js"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.mavenLocal

javaOptions in Test += "-Dlogger.resource=dev-logback.xml"