package com.github.calvin

package object models {
  case class Add(a: Int, b: Int, id: String)
  case class AddResult(result: Int)
  case class ErrorMessage(message: String)
}
