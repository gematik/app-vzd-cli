
FROM eclipse-temurin:21 AS builder

RUN apt-get update && apt-get install -y wget unzip

WORKDIR /app
ARG VERSION=3.2.0a4

# download release from github
RUN wget https://github.com/gematik/app-vzd-cli/releases/download/${VERSION}/vzd-cli-${VERSION}.zip
RUN unzip vzd-cli-${VERSION}.zip
RUN mv vzd-cli-${VERSION} vzd-cli

FROM eclipse-temurin:21
COPY --from=builder /app/vzd-cli /app/vzd-cli

ENTRYPOINT ["/app/vzd-cli/bin/vzd-cli"]