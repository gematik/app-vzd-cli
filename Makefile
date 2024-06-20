ktlint:
	ktlint -F */src/**/*.kt

dist: ktlint
	./gradlew clean build -x test
	java -jar ./vzd-cli/build/libs/vzd-cli-all.jar login tu
	./run_tests.sh

release: 
	$(eval VERSION := $(shell grep "^version=" gradle.properties | cut -d'=' -f2))
	gh release create ${VERSION}
	gh release upload ${VERSION} vzd-cli/build/distributions/vzd-cli-${VERSION}.zip
