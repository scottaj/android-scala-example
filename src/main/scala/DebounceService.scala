package org.example.suggest

import akka.actor._
import android.util.Log
import scala.concurrent.duration._

class DebounceService extends Actor {
  private var service: ActorRef = null

  def receive = {
    case debounce: Int =>
      Log.d("suggest:debouncer", debounce.toString())
      service = sender
      context.setReceiveTimeout(debounce milliseconds)

    case ReceiveTimeout =>
      Log.d("suggest:debouncer", "Finished Debouncing")
      service ! "DEBOUNCE_ENDED"
      context.setReceiveTimeout(Duration.Undefined)
  }
}
