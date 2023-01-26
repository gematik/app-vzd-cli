#FROM gradle:7.6-jdk19 AS build
#COPY --chown=gradle:gradle . /home/gradle/src
#WORKDIR /home/gradle/src/directory-bff
#RUN gradle buildFatJar --no-daemon -x test
#COPY --from=build /home/gradle/src/directory-bff/build/libs/directory-bff-all.jar /app/directory-bff-all.jar

FROM eclipse-temurin:19
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/libs/directory-bff-all.jar /app/directory-bff-all.jar
ENTRYPOINT ["java","-jar","/app/directory-bff-all.jar"]
