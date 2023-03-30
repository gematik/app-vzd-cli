export TEST_ACCESS_TOKEN=$(java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu token)
./gradlew test

