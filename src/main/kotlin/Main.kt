import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatMenuButton
import dev.inmo.tgbotapi.extensions.api.chat.modify.setChatMenuButton
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.get.getUserProfilePhotos
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.chat.CommonUser
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.row
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.Profile
import model.ProfileID
import java.io.File

val client = createSupabaseClient(
    supabaseUrl = "https://fecnldjxpserceyiifwt.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZlY25sZGp4cHNlcmNleWlpZnd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE2ODkwMjA5MjAsImV4cCI6MjAwNDU5NjkyMH0.yMRwZhIOlCo7EeTZ0lO5E_HJq-iV-ANZJFiNpwb3aeU"
) {
    install(Postgrest)
    install(Storage)
}


fun main() {
    val TOKEN = System.getenv("TELEGRAM_BOT_TOKEN")

    val bot = telegramBot(TOKEN)


    val waitTutorial = mutableListOf<CommonUser>()

    val sendMessage = mutableListOf<CommonUser>()

    CoroutineScope(Dispatchers.IO).launch {

        bot.buildBehaviourWithLongPolling {


            suspend fun tutorial(message: ContentMessage<TextContent>) {
                waitTutorial.add(message.from as CommonUser)

                this.reply(
                    message,
                    "Привет, ${message.from?.firstName ?: "новый пользователь"}✋. Я современный бот ${getMe().username!!.username}, который поможет тебе с обучением в ГУАП." +
                            "\n\nЯ проведу тебе небольшое обучение, если ты не хочешь его проходить введи /skip. А если ты хочешь пройти обучение, " +
                            "то я дам тебе 20 секунд, чтобы ты смог приготовиться к обучению."

                ).apply {

                    delay(20000)
                    if (waitTutorial.contains(message.from as CommonUser)) {
                        reply(this, "Первая функция - /switch\n\nВведите её и узнайте, что она делает")
                    }

                }
            }

            onCommand("skip") {
                waitTutorial.remove(it.from as CommonUser)
                reply(it, "Вы пропустили обучение.")
                setChatMenuButton(
                    it.chat.id,
                    menuButton = dev.inmo.tgbotapi.types.MenuButton.Default
                )
            }

            onCommand("tutorial") {
                tutorial(it)
            }




            onCommand("start") {

                // setChatMenuButton(it.chat.id, menuButton = dev.inmo.tgbotapi.types.MenuButton.Commands)
                try {
                    client.postgrest.from("profiles").select(columns = Columns.list("telegram_id")) {
                        eq("telegram_id", it.chat.id.chatId.toString())
                    }.decodeSingle<ProfileID>()
                    reply(it, "Добрый день, если вы хотите снова пройти обучение то напишите команду /tutorial")
                    return@onCommand
                } catch (e: Exception) {
                    val pathed = getFileAdditionalInfo(getUserProfilePhotos(it.from!! as CommonUser).photos[0].last())

                    val outfile = File(pathed.filePath)
                    runCatching {
                        bot.downloadFile(getUserProfilePhotos(it.from!! as CommonUser).photos[0].last(), outfile)
                    }.onFailure {
                        it.printStackTrace()
                    }
                    runCatching {
                        client.storage.from("profile_images").upload(it.from!!.id.chatId.toString() + ".jpg", outfile)

                        client.postgrest.from("profiles").insert(
                            Profile(
                                it.chat.id.chatId.toString(),
                                null,
                                null,
                                null,
                                false,
                                (it.from!!.firstName + " " + it.from!!.lastName),
                                "https://fecnldjxpserceyiifwt.supabase.co/storage/v1/object/public/profile_images/" + it.from!!.id.chatId.toString() + ".jpg"
                            )
                        )
                    }.onFailure {
                        it.printStackTrace()

                    }
                }

                tutorial(it)
            }

            onCommand("adminRequest") {
                if (waitTutorial.contains(it.from as CommonUser)) {
                    reply(
                        it,
                        "Ха-ха\uD83D\uDE01, попался, ты думал что так просто получишь права администратора. К сожалению нет. " +
                                "\nЕсли вы всё-таки явяетесь преподавателем, то после обучения вспомните об этой команде."
                    ).also {
                        reply(
                            it,
                            "Последняя команда - выход (/exit). Эта команда, полностью удаляет ваши данные с наших серверов. \n\nПопробуйте её в тестовом режиме."
                        )
                    }
                    return@onCommand
                }
                // Отправлять мне данные пользователя
            }

            onMessageDataCallbackQuery {
                if (it.data == "Да_1") {
                    edit(it.message.chat.id, it.message.messageId, "Вы точно уверены?", replyMarkup = inlineKeyboard {
                        row {
                            dataButton("Да", "Да_2")
                            dataButton("Нет", "Нет")
                        }
                    })
                    return@onMessageDataCallbackQuery
                }
                if (it.data == "Да_2") {
                    edit(it.message.chat.id, it.message.messageId, "Я НЕ СЛЫШУ", replyMarkup = inlineKeyboard {
                        row {
                            dataButton("ДАААААААА", "Да_3")
                            dataButton("Нет", "Нет")
                        }
                    })
                    return@onMessageDataCallbackQuery
                }
                if (it.data == "Да_3") {
                    client.postgrest.from("profiles").delete {
                        eq("telegram_id", it.message.chat.id.chatId)
                    }
                    edit(
                        it.message.chat.id,
                        it.message.messageId,
                        "Ваши данные удалены. Чтобы вернуться введите /start"
                    )
                    return@onMessageDataCallbackQuery
                }
                if (it.data == "Cancel") {
                    reply(it.message.chat.id, it.message.messageId, "Действие отменено")
                    sendMessage.remove(it.user)
                }
            }

            onCommand("sendAll") {
                val profile = client.postgrest.from("profiles").select {
                    eq("telegram_id", it.chat.id.chatId.toString())
                }.decodeSingle<Profile>()
                if (!profile.isAdmin) {
                    reply(it, "У вас нет прав для данной команды")
                }
                reply(it, "Отправьте сообщение, которое отправится всем студентам", replyMarkup = inlineKeyboard {
                    row {
                        dataButton("Отмена", "Cancel")
                    }
                })

            }

            onText {
                val profiles = client.postgrest.from("profiles").select {
                    eq("isAdmin", false)
                }.decodeList<Profile>()
                val p = it

                profiles.forEach {
                    send(
                        IdChatIdentifier.Companion.invoke(it.telegram_id.toLong()),
                        "Важное сообщение от ${p.from!!.firstName} ${p.from!!.lastName}:\n\n${p.content.text}"
                    )
                }
                if (sendMessage.contains(it.from)) {
                    reply(it, "Сообщение отправлено ${profiles.size} студентам")
                }
            }

            onCommand("exit") {
                if (waitTutorial.contains(it.from as CommonUser)) {
                    reply(it, "Обучение закончено. Вы теперь очень опытный пользователь.")
                    waitTutorial.remove(it.from as CommonUser)
                    return@onCommand
                }
                reply(it, "Вы точно хотите удалить ваши данные?", replyMarkup = inlineKeyboard {
                    row {
                        dataButton("Да", "Да_1")
                        dataButton("Нет", "Нет")
                    }
                })
            }





            onCommand("switch") {

                if (getChatMenuButton(it.chat.id) == dev.inmo.tgbotapi.types.MenuButton.Commands) {
                    reply(it, "Меняю на меню с приложением")
                    setChatMenuButton(
                        it.chat.id,
                        menuButton = dev.inmo.tgbotapi.types.MenuButton.Default
                    )
                } else {
                    reply(it, "Меняю на меню с командами")
                    setChatMenuButton(
                        it.chat.id,
                        menuButton = dev.inmo.tgbotapi.types.MenuButton.Commands
                    )
                }
                if (waitTutorial.contains(it.from as CommonUser)) {
                    reply(
                        it,
                        "Отлично, мы научились менять режим приложения. Большинство функционала реализовано на сайте. " +
                                "Но для отправки сообщений и различный функций с телеграмм существует текстовый режим с командами. " +
                                "Чтобы вернуться обратно введите комманду /switch снова."
                    ).also {
                        reply(
                            it,
                            "Возможно вы преподаватель?\n\n Введите команду /adminRequest, для того чтобы запросить права администратора. "
                        )
                    }

                }
            }
            // Будующая фишка
            /*onBaseInlineQuery {
                val page = it.offset.toIntOrNull() ?: 0

            }*/

        }.join()
    }
    while (true) {

    }

}
