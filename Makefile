ktlint:
	ktlint -F --disabled_rules=no-wildcard-imports,trailing-comma-on-declaration-site,standard:max-line-length */src/**/*.kt

dist: ktlint
	./gradlew clean build -x test
	java -jar ./vzd-cli/build/libs/vzd-cli-all.jar admin tu login
	./run_tests.sh

release: 
	$(eval VERSION := $(shell grep "^version=" gradle.properties | cut -d'=' -f2))
	gh release create ${VERSION}
	gh release upload ${VERSION} vzd-cli/build/distributions/vzd-cli-${VERSION}.zip
