import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatMenuButton
import dev.inmo.tgbotapi.extensions.api.chat.modify.setChatMenuButton
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.CommonUser
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.Profile

val client = createSupabaseClient(
    supabaseUrl = "https://fecnldjxpserceyiifwt.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZlY25sZGp4cHNlcmNleWlpZnd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE2ODkwMjA5MjAsImV4cCI6MjAwNDU5NjkyMH0.yMRwZhIOlCo7EeTZ0lO5E_HJq-iV-ANZJFiNpwb3aeU"
) {
    install(Postgrest)
}


fun main() {
    val TOKEN = System.getenv("TELEGRAM_BOT_TOKEN")

    val bot = telegramBot(TOKEN)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            client.postgrest.from("profiles").select(columns = Columns.list("telegram_id")) {
                eq("telegram_id", "1")
            }.decodeSingle<Profile>()

        } catch (e: Exception) {
            println(e)
            client.postgrest.from("profiles").insert(Profile(telegram_id = "1"))
        }
    }


    val waitTutorial = mutableListOf<CommonUser>()

    CoroutineScope(Dispatchers.IO).launch {

        bot.buildBehaviourWithLongPolling {


            suspend fun tutorial(message: ContentMessage<TextContent>) {
                waitTutorial.add(message.from as CommonUser)

                this.reply(
                    message,
                    "Привет, ${message.from?.firstName ?: "новый пользователь"}✋. Я современный бот ${getMe().username!!.username}, который поможет тебе с обучением в ГУАП." +
                            "\n\nЯ проведу тебе небольшое обучение, если ты не хочешь его проходить введи /skip. А если ты хочешь пройти обучение, " +
                            "то я дам тебе минуту, чтобы ты смог приготовиться к обучению."

                ).apply {

                    delay(60000)
                    if (waitTutorial.contains(message.from as CommonUser)) {
                        reply(this, "Отлично, основная работа")
                    }

                }
            }

            onCommand("skip") {
                waitTutorial.remove(it.from as CommonUser)
                reply(it, "Вы пропустили обучение, вы опытный пользьзователь")
                setChatMenuButton(
                    it.chat.id,
                    menuButton = dev.inmo.tgbotapi.types.MenuButton.Default
                )
            }

            onCommand("tutorial") {
                tutorial(it)
            }


            onCommand("start") {

                setChatMenuButton(it.chat.id, menuButton = dev.inmo.tgbotapi.types.MenuButton.Commands)

                try {
                    client.postgrest.from("profiles").select(columns = Columns.list("telegram_id")) {
                        eq("telegram_id", it.chat.id.chatId.toString())
                    }.decodeSingle<Profile>()
                    reply(it, "Добрый день, если вы хотите снова пройти обучение то напишите команду /tutorial")
                    return@onCommand
                } catch (e: Exception) {
                    client.postgrest.from("profiles").insert(Profile(telegram_id = it.chat.id.chatId.toString()))
                }

                tutorial(it)
            }

            onCommand("switch") {

                if (getChatMenuButton(it.chat.id) == dev.inmo.tgbotapi.types.MenuButton.Commands){
                    reply(it, "Меняю на меню с приложением")
                    setChatMenuButton(
                        it.chat.id,
                        menuButton = dev.inmo.tgbotapi.types.MenuButton.Default
                    )
                } else{
                    reply(it, "Меняю на меню с командами")
                    setChatMenuButton(
                        it.chat.id,
                        menuButton = dev.inmo.tgbotapi.types.MenuButton.Commands
                    )
                }
                if (waitTutorial.contains(it.from as CommonUser)) {
                    reply(it, "Отлично, мы научились менять режим приложения. Большинство функционала реализовано на сайте. " +
                            "Но для отправки сообщений и различный функций с телеграмм существует текстовый режим с командами. " +
                            "Чтобы вернуться обратно введите комманду /switch снова.")
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
