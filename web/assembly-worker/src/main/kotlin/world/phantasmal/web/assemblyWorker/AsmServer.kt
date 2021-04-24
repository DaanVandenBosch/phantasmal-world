package world.phantasmal.web.assemblyWorker

import mu.KotlinLogging
import world.phantasmal.web.shared.Throttle
import world.phantasmal.web.shared.messages.ClientMessage
import world.phantasmal.web.shared.messages.ClientNotification
import world.phantasmal.web.shared.messages.Request
import world.phantasmal.web.shared.messages.ServerMessage
import kotlin.time.measureTime

class AsmServer(
    private val asmAnalyser: AsmAnalyser,
    private val sendMessage: (ServerMessage) -> Unit,
) {
    private val messageQueue: MutableList<ClientMessage> = mutableListOf()
    private val messageProcessingThrottle = Throttle(wait = 100)

    fun receiveMessage(message: ClientMessage) {
        messageQueue.add(message)
        messageProcessingThrottle(::processMessages)
    }

    private fun processMessages() {
        // Split messages into ASM changes and other messages. Remove useless/duplicate
        // notifications.
        val asmChanges = mutableListOf<ClientNotification>()
        val otherMessages = mutableListOf<ClientMessage>()

        for (message in messageQueue) {
            when (message) {
                is ClientNotification.SetAsm -> {
                    // All previous ASM change messages can be discarded when the entire ASM has
                    // changed.
                    asmChanges.clear()
                    asmChanges.add(message)
                }

                is ClientNotification.UpdateAsm ->
                    asmChanges.add(message)

                else ->
                    otherMessages.add(message)
            }
        }

        messageQueue.clear()

        // Process ASM changes first.
        processAsmChanges(asmChanges)
        otherMessages.forEach(::processMessage)
    }

    private fun processAsmChanges(messages: List<ClientNotification>) {
        if (messages.isNotEmpty()) {
            val time = measureTime {
                for (message in messages) {
                    when (message) {
                        is ClientNotification.SetAsm ->
                            asmAnalyser.setAsm(message.asm, message.inlineStackArgs)

                        is ClientNotification.UpdateAsm ->
                            asmAnalyser.updateAsm(message.changes)

                        else ->
                            // Should be processed by processMessage.
                            logger.error { "Unexpected ${message::class.simpleName}." }
                    }
                }

                asmAnalyser.processAsm().forEach(sendMessage)
            }

            logger.trace {
                "Processed ${messages.size} assembly changes in ${time.inMilliseconds}ms."
            }
        }
    }

    private fun processMessage(message: ClientMessage) {
        val time = measureTime {
            when (message) {
                is ClientNotification.SetAsm,
                is ClientNotification.UpdateAsm ->
                    // Should have been processed by processAsmChanges.
                    logger.error { "Unexpected ${message::class.simpleName}." }

                is Request -> processRequest(message)
            }
        }

        logger.trace { "Processed ${message::class.simpleName} in ${time.inMilliseconds}ms." }
    }

    private fun processRequest(message: Request) {
        val response = when (message) {
            is Request.GetCompletions ->
                asmAnalyser.getCompletions(message.id, message.lineNo, message.col)

            is Request.GetSignatureHelp ->
                asmAnalyser.getSignatureHelp(message.id, message.lineNo, message.col)

            is Request.GetHover ->
                asmAnalyser.getHover(message.id, message.lineNo, message.col)

            is Request.GetDefinition ->
                asmAnalyser.getDefinition(message.id, message.lineNo, message.col)

            is Request.GetLabels ->
                asmAnalyser.getLabels(message.id)

            is Request.GetHighlights ->
                asmAnalyser.getHighlights(message.id, message.lineNo, message.col)
        }

        sendMessage(response)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
