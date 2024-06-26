package com.example.telegrambot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.KeyboardButton
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage

sealed class Command(val value: String) {
    data object Start : Command("/start")
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
            updates.mapNotNull { it.message() }
                .onEach { message ->
                    restartIfNeed(message)
                    handleMessageByState(message)
                }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    private fun restartIfNeed(message: Message) {
        val text = message.text()
        when (text) {
            Command.Start.value -> Bot.updateUserState(message.chat().id(), State.Start)
            Command.Menu.value -> Bot.updateUserState(message.chat().id(), State.Idle)
        }
    }

    private fun handleMessageByState(message: Message) {
        val chatId = message.chat().id()
        when (Bot.getUserState(chatId)) {
            State.Start -> {
                sendMessage(
                    chatId,
                    "Привет. Это бот сбора тем и вопросов для обсуждения на TechTalk встречах \uD83D\uDE09."
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
                saveQuestion(message)
                sendMessage(chatId, "Вопрос отправлен \uD83D\uDC4C")
                Bot.updateUserState(chatId, State.Idle)
                handleMessageByState(message)
            }

            State.SaveTheme -> {
                saveTheme(message)
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

fun saveQuestion(message: Message) {
    val text = """
        #Вопрос
        Автор: ${message.from().firstName()} ${message.from().lastName()}
        --- 
        ${message.text()}
    """.trimIndent()
    sendMessage(Bot.adminId, text)
}

private fun saveTheme(message: Message) {
    val text = """
        #Тема
        Автор: ${message.from().firstName()} ${message.from().lastName()}
        --- 
        ${message.text()}
    """.trimIndent()
    sendMessage(Bot.adminId, text)
}

fun main() {
    // Токен бота который можно получить у https://t.me/BotFather
    val botToken = System.getenv()["BOT_TOKEN"]

    // Id чата между ботом и админом которому будут сыпаться все заявки
    val adminId = System.getenv()["ADMIN_ID"]

    requireNotNull(botToken) { "BOT_TOKEN environment variable should be initialized" }
    requireNotNull(adminId) { "ADMIN_ID environment variable should be initialized" }

    val bot = TechTalkBot(botToken, adminId.toLong())
    bot.start()
}
