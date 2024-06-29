package io.chat.frontend

import DataEntry
import NewMessageEvent
import io.ktor.client.engine.js.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.transport.ktor.websocket.client.WebSocketClientTransport
import kotlinx.browser.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.*


suspend fun main() = coroutineScope {
    val transport = WebSocketClientTransport(Js, "127.0.0.1", path = "rsocket", port = 8080)
    val connector = RSocketConnector()

    val rsocket: RSocket = connector.connect(transport)
    val dataFlow: Flow<DataEntry> = rsocket.requestStream(Payload.Empty).map {
        Json.decodeFromString<DataEntry>(it.data.readText())
    }

    launch {
        dataFlow.collect { dataEntry -> writeMessage("[user ${dataEntry.userId}]: ${dataEntry.message}") }
    }

    suspend fun sendEvent(event: NewMessageEvent) {
        rsocket.fireAndForget(buildPayload { data(Json.encodeToString(event)) })
    }

    val sendButton = document.getElementById("sendButton") as HTMLElement
    val commandInput = document.getElementById("commandInput") as HTMLInputElement

    sendButton.addEventListener("click", {
        val message = commandInput.value
        launch { sendEvent(NewMessageEvent(message)) }
        commandInput.value = ""
    })
}


fun writeMessage(message: String) {
    val line = document.createElement("p") as HTMLElement
    line.className = "message"
    line.textContent = message

    val messagesBlock = document.getElementById("messages") as HTMLElement
    messagesBlock.appendChild(line)
    messagesBlock.scrollTop = line.offsetTop.toDouble()
}
