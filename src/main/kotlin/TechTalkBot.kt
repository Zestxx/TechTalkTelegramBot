package com.example.telegrambot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.KeyboardButton
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.request.ForwardMessage
import com.pengrad.telegrambot.request.SendMessage

sealed class Command(val value: String) {
    data object ThrowTheme : Command("Вкинуть тему")
    data object ThrowQuestion : Command("Вкинуть вопрос")
    data object Menu : Command("В меню")
}

sealed class State {
    data object Start : State()
    data object Idle : State()
    data object InputCommand : State()
    data object ThrowTheme : State()
    data object ThrowQuestion : State()
    data object SaveTheme : State()
    data object SaveQuestion : State()
}

object Bot {
    private var botToken: String? = null
    private val userStates: MutableMap<Long, State> = mutableMapOf()

    var adminId: Long = 0
        private set

    fun initEnv(botToken: String, adminId: Long) {
        this.botToken = botToken
        this.adminId = adminId
    }

    val instance by lazy { TelegramBot(botToken) }

    fun getUserState(userId: Long): State {
        val state = userStates[userId] ?: State.Start.also { userStates[userId] = it }
        return state
    }

    fun updateUserState(userId: Long, state: State) {
        userStates[userId] = state
    }
}

class TechTalkBot(botToken: String, adminId: Long) {

    init {
        Bot.initEnv(botToken, adminId)
    }

    fun start() {
        Bot.instance.setUpdatesListener { updates ->
            updates.forEach { update ->
                val message = update.message() ?: return@forEach
                handleMessageByState(message)
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    private fun handleMessageByState(message: Message) {
        val chatId = message.chat().id()
        when (Bot.getUserState(chatId)) {
            State.Start -> {
                sendMessage(
                    chatId,
                    "Привет. Это бот для сбора тем и вопросов для обсуждения на TechTalk встречах \uD83D\uDE09."
                )
                Bot.updateUserState(chatId, State.Idle)
                handleMessageByState(message)
            }

            State.Idle -> {
                sendMessage(
                    chatId, "Выбери действие:", listOf(
                        KeyboardButton(Command.ThrowTheme.value),
                        KeyboardButton(Command.ThrowQuestion.value)
                    )
                )
                Bot.updateUserState(chatId, State.InputCommand)
            }

            State.ThrowQuestion -> {
                sendMessage(chatId, "Предложи вопрос", listOf(KeyboardButton(Command.Menu.value)))
                Bot.updateUserState(chatId, State.SaveQuestion)
            }

            State.ThrowTheme -> {
                sendMessage(chatId, "Предложи тему", listOf(KeyboardButton(Command.Menu.value)))
                Bot.updateUserState(chatId, State.SaveTheme)
            }

            State.SaveQuestion -> {
                forwardMessageTo(message, Bot.adminId)
                sendMessage(chatId, "Вопрос отправлен \uD83D\uDC4C")
                Bot.updateUserState(chatId, State.Idle)
                handleMessageByState(message)
            }

            State.SaveTheme -> {
                forwardMessageTo(message, Bot.adminId)
                sendMessage(chatId, "Тема отправлена \uD83D\uDC4D")
                Bot.updateUserState(chatId, State.Idle)
                handleMessageByState(message)
            }

            State.InputCommand -> handleCommand(message)
        }
    }

    private fun handleCommand(message: Message) {
        val chatId = message.chat().id()
        when (message.text()) {
            Command.ThrowTheme.value -> {
                Bot.updateUserState(chatId, State.ThrowTheme)
                handleMessageByState(message)
            }

            Command.ThrowQuestion.value -> {
                Bot.updateUserState(chatId, State.ThrowQuestion)
                handleMessageByState(message)
            }

            Command.Menu.value -> {
                Bot.updateUserState(chatId, State.Idle)
                handleMessageByState(message)
            }
        }
    }
}

fun sendMessage(chatId: Long, text: String, buttons: List<KeyboardButton> = emptyList()) {
    val request = SendMessage(chatId, text)
        .apply {
            if (buttons.isNotEmpty()) {
                replyMarkup(ReplyKeyboardMarkup(buttons.toTypedArray()))
            }
        }
    Bot.instance.execute(request)
}

fun forwardMessageTo(message: Message, toChatId: Long) {
    Bot.instance.execute(ForwardMessage(message.chat().id(), toChatId, message.messageId()))
}

fun main(args: Array<String>) {
    // Токен бота который можно получить у https://t.me/BotFather
    val botToken = args[0]

    // Id чата между ботом и админом которому будут сыпаться все заявки
    val adminId = args[1]

    val bot = TechTalkBot(botToken, adminId.toLong())
    bot.start()
}
