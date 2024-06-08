FROM gradle:jdk17

WORKDIR /app

COPY build.gradle.kts gradle.properties settings.gradle.kts /app/

COPY gradlew /app/
COPY gradle /app/gradle

COPY src /app/src

RUN ./gradlew build

CMD ["java", "-jar", "build/libs/tech_talk_bot-0.1.jar", "${BOT_TOKEN}", "${ADMIN_ID}"]

