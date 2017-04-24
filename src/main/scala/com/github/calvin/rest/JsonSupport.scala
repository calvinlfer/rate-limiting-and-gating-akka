package com.github.calvin.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.calvin.models._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val addFormat: RootJsonFormat[Add] = jsonFormat3(Add)
  implicit val addResultFormat: RootJsonFormat[AddResult] = jsonFormat1(AddResult)
  implicit val errorMsgFormat: RootJsonFormat[ErrorMessage] = jsonFormat1(ErrorMessage)
}
