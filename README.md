# TechTalkTelegramBot

Бот для сбора тем и вопросов для еженедельных встреч команды. 
Для запуска на сервере используется docker.

## Сборка образа:
1. Клонируем репозиторий `git clone git@github.com:Zestxx/TechTalkTelegramBot.git`
2. Переходим в папку проекта
3. Собираем образ `docker build -t tech_talk_bot:01 .`
4. Запускаем образ `docker run -e BOT_TOKEN=Токен_бота -e ADMIN_ID=ID_чата_админа -d --name tech_talk_bot_instance --restart always tech_talk_bot:01`
