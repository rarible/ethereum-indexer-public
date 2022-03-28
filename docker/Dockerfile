FROM bellsoft/liberica-openjdk-alpine:8

RUN apk add curl

RUN addgroup -S rarible && adduser -S rarible -G rarible
USER rarible:rarible

WORKDIR /usr/app
COPY ./target/boot/*.jar application.jar

CMD java $JAVA_OPTIONS -jar application.jar