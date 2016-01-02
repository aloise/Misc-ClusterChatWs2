package utils

import javax.inject.{Singleton, Inject}
import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import play.api.inject.{Binding, Module, ApplicationLifecycle}
import play.api.{Configuration, Environment, Application}
import play.modules.reactivemongo.{DefaultReactiveMongoApi, ReactiveMongoModule, ReactiveMongoApi}

import scala.concurrent.ExecutionContext

/**
 * User: aloise
 * Date: 12.11.15
 * Time: 20:46
 */


class ApplicationInitClass @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends AbstractModule {

  override def configure(): Unit = {
    models.ChatRooms.closeOutdatedChatRooms()
    println("Hello")

  }


}
