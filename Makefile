ktlint:
	ktlint -F */src/**/*.kt

dist: ktlint dockerfile
	./gradlew clean build -x test
	java -jar ./vzd-cli/build/libs/vzd-cli-all.jar login tu
	./run_tests.sh

release: dockerfile
	$(eval VERSION := $(shell grep "^version=" gradle.properties | cut -d'=' -f2))
	gh release create ${VERSION}
	gh release upload ${VERSION} vzd-cli/build/distributions/vzd-cli-${VERSION}.zip

gui:
	cd directory-app && npm install && ng build
	rm -rf vzd-cli/src/main/resources/directory-app
	mv directory-app/dist/directory-app vzd-cli/src/main/resources

dockerfile: vzd-cli/Dockerfile.tmpl
	$(eval VERSION := $(shell grep "^version=" gradle.properties | cut -d'=' -f2))
	sed 's/ARG VERSION=.*/ARG VERSION=${VERSION}/' vzd-cli/Dockerfile.tmpl > Dockerfile