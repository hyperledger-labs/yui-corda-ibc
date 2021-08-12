.PHONY: build clean buildClient
.PHONY: buildImage buildClientImage buildLightclientdImage
.PHONY: deployNodes upNodes downNodes
.PHONY: prepareHostA startServerA shutdownServerA startServerBankA shutdownServerBankA
.PHONY: prepareHostB startServerB shutdownServerB
.PHONY: executeTest executeOldTest
.PHONY: test oldTest

NAME			:= yui-corda-ibc
CLIENT_NAME		:= yui-corda-ibc-client
LIGHTCLIENTD_NAME	:= yui-corda-ibc-lightclientd

CLIENT		?= ./rust/target/release/$(CLIENT_NAME)

DOCKER_REG	?= ""
DOCKER_REPO	?= ""
DOCKER_TAG	?= :latest

build:
	./gradlew -x test build

clean:
	./gradlew clean

buildClient:
	cd rust && cargo clean && cargo build --release

buildImage:
	docker build -f ./Dockerfiles/$(NAME)/Dockerfile -t $(DOCKER_REG)$(DOCKER_REPO)$(NAME)$(DOCKER_TAG) \
		--build-arg CLIENT_NAME=$(CLIENT_NAME) \
		--build-arg DOCKER_REG=$(DOCKER_REG) \
		--build-arg DOCKER_REPO=$(DOCKER_REPO) \
		--build-arg DOCKER_TAG=$(DOCKER_TAG) \
		.

buildClientImage:
	docker build -f ./Dockerfiles/$(CLIENT_NAME)/Dockerfile -t $(DOCKER_REG)$(DOCKER_REPO)$(CLIENT_NAME)$(DOCKER_TAG) .

buildLightclientdImage:
	docker build -f ./Dockerfiles/$(LIGHTCLIENTD_NAME)/Dockerfile -t $(DOCKER_REG)$(DOCKER_REPO)$(LIGHTCLIENTD_NAME)$(DOCKER_TAG) .

deployNodes:
	./gradlew deployNodes

upNodes:
	./build/nodes/runnodes --headless
	for p in `seq  2222 1  2223`; do while ! nc -z localhost $$p; do sleep 1; done; done
	for p in `seq 10003 3 10006`; do while ! nc -z localhost $$p; do sleep 1; done; done

downNodes:
	-sshpass -p test ssh -p 2222 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2223 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown

prepareHost:
	./gradlew :grpc-adapter:runServer --args 'localhost 10003 user1 test 9999' &
	while ! nc -z localhost 9999; do sleep 1; done
	$(CLIENT) genesis create-genesis -e http://localhost:9999 -p PartyA,Notary > base-hash.txt
	$(CLIENT) admin shutdown         -e http://localhost:9999
	./gradlew :grpc-adapter:runServer --args "localhost 10003 user1 test 9999 `cat base-hash.txt`" &
	while ! nc -z localhost 9999; do sleep 1; done
	$(CLIENT) host create-host           -e http://localhost:9999
	$(CLIENT) cash-bank create-cash-bank -e http://localhost:9999 -b `$(CLIENT) node address-from-name -e http://localhost:9999 -n Notary`
	$(CLIENT) cash-bank allocate-cash    -e http://localhost:9999 -o `$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA` -c USD -a 100
	$(CLIENT) admin shutdown             -e http://localhost:9999

runServer:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash.txt`"

startServer:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash.txt`" &
	while ! nc -z localhost 9999; do sleep 1; done

shutdownServer:
	$(CLIENT) admin shutdown -e http://localhost:9999

startServerBank:
	./gradlew :grpc-adapter:runServer --args "localhost 10003 user1 test 29999 `cat base-hash.txt`" &
	while ! nc -z localhost 29999; do sleep 1; done

shutdownServerBank:
	$(CLIENT) admin shutdown -e http://localhost:29999

upA:
	docker run -d --rm --name yui-corda-ibc-a -p 9999:9999 -p 29999:29999 yui-corda-ibc:latest
	while ! wget -q -O - 'localhost:9999'; do sleep 1; done
	while ! wget -q -O - 'localhost:29999'; do sleep 1; done

upB:
	docker run -d --rm --name yui-corda-ibc-b -p 9998:9999 -p 29998:29999 yui-corda-ibc:latest
	while ! wget -q -O - 'localhost:9998'; do sleep 1; done
	while ! wget -q -O - 'localhost:29998'; do sleep 1; done

downA:
	docker stop yui-corda-ibc-a

downB:
	docker stop yui-corda-ibc-b

executeOldTest:
	./gradlew :grpc-adapter:runClient --args "executeTest localhost:9999 localhost:9998 localhost:29999 `$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA` `$(CLIENT) node address-from-name -e http://localhost:9998 -n PartyA`"

executeTest:
	$(CLIENT) client create-clients \
		--client-id-a corda-ibc-0 \
		--client-id-b corda-ibc-0
	$(CLIENT) connection handshake \
		--client-id-a corda-ibc-0 \
		--client-id-b corda-ibc-0 \
		--connection-id-a connection-0 \
		--connection-id-b connection-0
	$(CLIENT) channel handshake \
		--connection-id-a connection-0 \
		--connection-id-b connection-0 \
		--channel-id-a channel-0 \
		--channel-id-b channel-0

test: upA upB executeTest downA downB

oldTest: upA upB executeOldTest downA downB
