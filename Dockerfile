FROM openjdk:8-buster
ARG PORT
ENV PORT $PORT

COPY . /usr/src/app
WORKDIR /usr/src/app
EXPOSE $PORT/tcp

RUN ./gradlew clean :lightclientd:build

CMD ./gradlew :lightclientd:runLightclientd --args $PORT
