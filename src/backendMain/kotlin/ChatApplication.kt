package io.chat.backend

import DataEntry
import NewMessageEvent
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.ktor.server.RSocketSupport
import io.rsocket.kotlin.ktor.server.rSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
        val dataFlow = MutableSharedFlow<DataEntry>(10)

        val usersCounter = AtomicInteger()

        install(WebSockets)
        install(RSocketSupport)
        install(Sessions) {
            cookie<ChatSession>("chat_session", SessionStorageMemory())
        }

        routing {

            static {
                defaultResource("index.html", "web")
                resources("web")
            }

            rSocket("rsocket") {
                this@embeddedServer.log.info(config.setupPayload.data.readText())

                val session = ChatSession(usersCounter.incrementAndGet())

                RSocketRequestHandler {
                    fireAndForget { payload ->
                        val newMessageEvent = Json.decodeFromString<NewMessageEvent>(payload.data.readText())
                        dataFlow.emit(
                            DataEntry(session.id, newMessageEvent.message)
                        )
                    }
                    requestStream { _: Payload ->
                        dataFlow.map { event ->
                            buildPayload {
                                data(Json.encodeToString<DataEntry>(event))
                            }
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}


data class ChatSession(val id: Int)