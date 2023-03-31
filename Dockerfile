FROM gradle:7.3.3-jdk11-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:11

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar
ENV CHESS_DAILY_SEED=$CHESS_DAILY_SEED
ENV SLACK_TOKEN=$SLACK_TOKEN
ENV SLACK_SIGNING_SECRET=$SLACK_SIGNING_SECRET
ENV OPEN_API_KEY=$OPEN_API_KEY
ENTRYPOINT ["java", "-jar", "/app/spring-boot-application.jar"]