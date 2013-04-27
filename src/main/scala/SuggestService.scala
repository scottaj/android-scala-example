package org.example.suggest

import akka.actor._
import android.util.Log
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.Scanner
import scala.util.parsing.json._

class SuggestService extends Actor {
  private val debouncer = context.actorOf(Props[DebounceService])
  private val apiBaseUrl = "http://suggestqueries.google.com/complete/search?output=firefox&q="
  private var activity: ActorRef = null
  private var query: String = null

  def receive = {
    case (debounce: Int, input: String) =>
      Log.d("suggest:service", input)
      debouncer ! debounce
      activity = sender
      query = input

    case msg =>
      Log.d("suggest:service", "Making API request")
      activity ! makeApiRequest(query)
  }

  private def makeApiRequest(query: String): List[String] = {
    val url = new URL(apiBaseUrl + URLEncoder.encode(query))
    val conn = makeApiConnection(url)
    val scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A")
    val response = if(scanner.hasNext()) scanner.next() else ""
    conn.disconnect

    Log.d("suggest:service:request", response)

    JSON.parseFull(response) match {
      case Some(list: List[_]) =>
        list.tail.head.asInstanceOf[List[String]]
      case _ =>
        List()
    }
  }

  private def makeApiConnection(url: URL): HttpURLConnection = {
    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    conn.setReadTimeout(10000)
    conn.setConnectTimeout(15000)
    conn.setRequestMethod("GET")
    conn.connect()
    conn
  }
}
