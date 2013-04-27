import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "Suggest",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.10.1",
    platformName in Android := "android-10"
  )

  val proguardSettings = Seq (
    useProguard in Android := true,

    // Config to stop Akka from dying
    proguardOptimizations in Android += "-keep class com.typesafe.**",
    proguardOptimizations in Android += "-keep class akka.**",
    proguardOptimizations in Android += "-keep class scala.collection.immutable.StringLike {*;}",
    proguardOptimizations in Android += "-keepclasseswithmembers class * {public <init>(java.lang.String, akka.actor.ActorSystem$Settings, akka.event.EventStream, akka.actor.Scheduler, akka.actor.DynamicAccess);}",
    proguardOptimizations in Android += "-keepclasseswithmembers class * {public <init>(akka.actor.ExtendedActorSystem);}",
    proguardOptimizations in Android += "-keep class scala.collection.SeqLike {public protected *;}",

    // Standard Android options
    proguardOptimizations in Android += "-keep public class * extends android.app.Application",
    proguardOptimizations in Android += "-keep public class * extends android.app.Service",
    proguardOptimizations in Android += "-keep public class * extends android.content.BroadcastReceiver",
    proguardOptimizations in Android += "-keep public class * extends android.content.ContentProvider",
    proguardOptimizations in Android += "-keep public class * extends android.view.View {public <init>(android.content.Context); public <init>(android.content.Context, android.util.AttributeSet); public <init>(android.content.Context, android.util.AttributeSet, int); public void set*(...);}",
    proguardOptimizations in Android += "-keepclasseswithmembers class * {public <init>(android.content.Context, android.util.AttributeSet);}",
    proguardOptimizations in Android += "-keepclasseswithmembers class * {public <init>(android.content.Context, android.util.AttributeSet, int);}",
    proguardOptimizations in Android += "-keepclassmembers class * extends android.content.Context {public void *(android.view.View); public void *(android.view.MenuItem);} ",
    proguardOptimizations in Android += "-keepclassmembers class * implements android.os.Parcelable {static android.os.Parcelable$Creator CREATOR;}",
    proguardOptimizations in Android += "-keepclassmembers class **.R$* {public static <fields>;}"
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me",
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0",
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      libraryDependencies += "org.scaloid" % "scaloid" % "1.0_8_2.10"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "Suggest",
    file("."),
    settings = General.fullAndroidSettings
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings ++
               AndroidTest.androidSettings ++
               General.proguardSettings ++ Seq (
      name := "SuggestTests"
    )
  ) dependsOn main
}
