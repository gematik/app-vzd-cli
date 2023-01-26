$(eval VERSION := $(shell grep "^version=" ../gradle.properties | cut -d'=' -f2))

docker:
		docker build -t spilikin/directory-bff:latest .
		docker tag spilikin/directory-bff:latest spilikin/directory-bff:${VERSION}

dockerhub: docker
		docker push spilikin/directory-bff:latest 
		docker push spilikin/directory-bff:${VERSION}
