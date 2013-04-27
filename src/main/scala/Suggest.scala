package org.example.suggest

import akka.actor._
import akka.actor.ActorDSL._
import android.app.{Activity, SearchManager}
import android.content.Intent
import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.AdapterView.OnItemClickListener
import org.scaloid.common._
import android.widget.ArrayAdapter
import java.util.ArrayList


class Suggest extends SActivity with TypedActivity {
  private var adapter: ArrayAdapter[String] = null
  private implicit val system = ActorSystem("suggest")
  private val service = system.actorOf(Props[SuggestService])
  private var layout: Layout = null

  private val serviceHook = actor(new Act {
    become {
      case results: List[String] =>
        updateResultsList(results)

      case msg =>
        Log.d("suggest:serviceHook", "Pinging Service")
        setWaitingMessage()
        service ! (500, layout.inputText.getText.toString)
    }
  })

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)

    layout = new Layout()
    setContentView(layout)

    // Scaloid has an SArray adapter, but it's items ae immutable
    adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, new ArrayList[String])
    layout.resultList.setAdapter(adapter)

    setListeners()
  }

  private def doSearch(query: String) {
    val intent = new Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query)
    startActivity(intent)
  }

  private def setWaitingMessage() {
    runOnUiThread(new Runnable() {
      def run() {
        adapter.clear()
        adapter.add("Fetching Suggestions...")
        adapter.notifyDataSetChanged()
      }
    })
  }

  private def updateResultsList(results: List[String]) {
    // UI Code must run on the UI thread
    runOnUiThread(new Runnable() {
      def run() {
        Log.d("suggest:update", "updating layout")
        adapter.clear()
        results foreach (adapter.add(_))
        adapter.notifyDataSetChanged()
      }
    })
  }

  private def setListeners() {
    startTextListener()
    startClickListener()
  }

  private def startTextListener() {
    val textWatcher = new TextWatcher {
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        serviceHook ! "MAKE_REQUEST"
      }

      // Need to implement these members
      def beforeTextChanged(x$1: CharSequence, x$2: Int, x$3: Int, x$4: Int) {}
      def afterTextChanged(x$1: android.text.Editable) {}
    }

    layout.inputText.addTextChangedListener(textWatcher)
  }

  private def startClickListener() {

    val clickListener = new OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
        val query = parent.getItemAtPosition(position).asInstanceOf[String]
        doSearch(query)
      }
    }

    layout.resultList.setOnItemClickListener(clickListener)
  }
}
