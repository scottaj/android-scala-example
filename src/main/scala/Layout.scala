package org.example.suggest

import org.scaloid.common._

class Layout(implicit context: SContext) extends SLinearLayout {

  lazy val inputText = new SEditText()
    .hint(R.string.input_hint)
    .padding(10 dip)
    .textSize(18 sp)

  lazy val resultList = new SListView()
    .<<(FILL_PARENT, WRAP_CONTENT).>>

  // Set layout parameters
  this
    .padding(10 dip)
    .orientation(VERTICAL)

  // Add widgets to layout
  STextView("Enter Search Query")
  this += inputText
  STextView("Suggestions:")
  this += resultList
}