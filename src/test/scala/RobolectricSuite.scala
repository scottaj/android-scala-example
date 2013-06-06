import android.net.Uri__FromAndroid
import com.xtremelabs.robolectric._
import com.xtremelabs.robolectric.bytecode._
import com.xtremelabs.robolectric.internal.RealObject
import com.xtremelabs.robolectric.res._
import com.xtremelabs.robolectric.shadows._
import com.xtremelabs.robolectric.util.{DatabaseConfig, SQLiteMap}
import com.xtremelabs.robolectric.util.DatabaseConfig._
import java.io.{File, FileOutputStream, PrintStream}
import org.scalatest._


trait RobolectricSuite extends Suite {
  lazy val instrumentedClass = RobolectricSuite.classLoader.bootstrap(this.getClass)
  lazy val instrumentedInstance = instrumentedClass.newInstance().asInstanceOf[RobolectricSuite]
  lazy val robolectricConfig = new RobolectricConfig(new File("src/main"))
  lazy val defaultDatabaseMap: DatabaseMap = new com.xtremelabs.robolectric.util.SQLiteMap()

  lazy val resourceLoader = {
    new ResourceLoader(robolectricConfig.getRealSdkVersion,
      Class.forName(robolectricConfig.getRClassName),
      robolectricConfig.getResourceDirectory,
      robolectricConfig.getAssetsDirectory)
  }

  override def run(testName: Option[String],
                   reporter: Reporter,
                   stopper: Stopper,
                   filter: Filter,
                   configMap: Map[String, Any],
                   distributor: Option[Distributor],
                   tracker: Tracker) {
    if (!isInstrumented) {
      instrumentedInstance.beforeTest()
      instrumentedInstance.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
      instrumentedInstance.afterTest()
    } else {
      super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
    }
  }

  def isInstrumented = getClass.getClassLoader.getClass.getName.contains(classOf[RobolectricClassLoader].getName)

  protected def beforeTest() {
    setupApplicationState()
  }
  protected def afterTest()  {}
  protected def resetStaticState()  {}
  protected def bindShadowClasses() {}

  protected def setupApplicationState() {
      robolectricConfig.validate()
      setupLogging()
      Robolectric.bindDefaultShadowClasses()
      bindShadowClasses()

      Robolectric.resetStaticState()
      resetStaticState()

      DatabaseConfig.setDatabaseMap(setupDatabaseMap(getClass, defaultDatabaseMap))
      Robolectric.application = ShadowApplication.bind(new ApplicationResolver(robolectricConfig).resolveApplication(), resourceLoader)
  }

  protected def setupLogging() {
    val logging = System.getProperty("robolectric.logging")
    if (logging != null && ShadowLog.stream == null) {
      ShadowLog.stream = logging match {
        case "stdout" => System.out
        case "stderr" => System.err
        case _ => {
          val file = new PrintStream(new FileOutputStream(logging))
          Runtime.getRuntime.addShutdownHook(new Thread() {
            override def run() {
              try {
                file.close();
              } catch { case e:Exception =>  }
            }
          })
          file
        }
      }
    }
  }

  protected def setupDatabaseMap(testClass: Class[_], map: DatabaseMap):DatabaseMap = {
    if (testClass.isAnnotationPresent(classOf[UsingDatabaseMap])) {
      val usingMap = testClass.getAnnotation(classOf[UsingDatabaseMap])
      if (usingMap.value() != null) {
        Robolectric.newInstanceOf(usingMap.value())
      } else if (map == null) {
        throw new RuntimeException("UsingDatabaseMap annotation value must provide a class implementing DatabaseMap")
      } else {
        map
      }
    } else {
      map
    }
  }
}

object RobolectricSuite {
  lazy val classHandler = ShadowWrangler.getInstance
  lazy val classLoader = {
    val loader = new RobolectricClassLoader(classHandler)
    loader.delegateLoadingOf("org.scalatest.")
    loader.delegateLoadingOf("org.mockito.")
    loader.delegateLoadingOf("scala.")

    List(
      classOf[Uri__FromAndroid],
      classOf[RobolectricSuite],
      classOf[RealObject],
      classOf[ShadowWrangler],
      classOf[RobolectricConfig],
      classOf[DatabaseMap],
      classOf[android.R]
    ).foreach {
      classToDelegate => loader.delegateLoadingOf(classToDelegate.getName)
    }
    loader
  }
}