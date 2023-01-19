ktlint:
		ktlint -F --disabled_rules=no-wildcard-imports

dist: ktlint
		./gradlew clean build -x test
		java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu login
		TEST_ACCESS_TOKEN := $(shell java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu token)
		./gradlew build
