FROM gradle:9.5.1-jdk25 AS build
RUN mkdir -p /home/gradle
ENV GRADLE_USER_HOME=/home/gradle
COPY --chown=gradle:gradle build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/
COPY --chown=gradle:gradle src /home/gradle/src
COPY --chown=gradle:gradle gradle /home/gradle/gradle
WORKDIR /home/gradle
RUN gradle installDist --no-daemon


FROM eclipse-temurin:25-jre-jammy
RUN apt update && apt upgrade -y && apt install -y curl vim
RUN useradd -M app
RUN mkdir -p /app
RUN chown -R app:app /app
COPY --from=build --chown=app:app /home/gradle/build/install/ParkeerAssistent /app/
RUN chmod +x /app/bin/ParkeerAssistent

EXPOSE 3000
ENTRYPOINT ["/app/bin/ParkeerAssistent"]
