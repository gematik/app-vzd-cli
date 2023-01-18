lint:
	ktlint -F --disabled_rules=no-wildcard-imports

dist:
	ktlint -F --disabled_rules=no-wildcard-imports
	./gradlew clean
	./gradlew build -x test
	java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu login
	export TEST_ACCESS_TOKEN=$(shell java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu token)
	./gradlew build
