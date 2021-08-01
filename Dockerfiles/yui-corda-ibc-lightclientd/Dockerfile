FROM openjdk:8-buster
ARG PORT=60000
ENV PORT $PORT
WORKDIR /usr/src/app
COPY . .
RUN ./gradlew clean :lightclientd:build

CMD ./gradlew :lightclientd:runLightclientd --args $PORT
