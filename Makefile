ktlint:
	ktlint -F --disabled_rules=no-wildcard-imports */src/**/*.kt

dist: ktlint
	./gradlew clean build -x test
	java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu login
	TEST_ACCESS_TOKEN=$(shell java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu token) ./gradlew test

release: 
	$(eval VERSION := $(shell grep "^version=" gradle.properties | cut -d'=' -f2))
	gh release create ${VERSION}
	gh release upload ${VERSION} vzd-cli/build/distributions/vzd-cli-${VERSION}.zip
