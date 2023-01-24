NAME			:= yui-corda-ibc
CLIENT_NAME		:= yui-corda-ibc-client
LIGHTCLIENTD_NAME	:= yui-corda-ibc-lightclientd

CLIENT		?= ./rust/target/debug/$(CLIENT_NAME)

DOCKER_REG	?= ""
DOCKER_REPO	?= ""
DOCKER_TAG	?= :latest

.PHONY: build
build:
	./gradlew -x test build

.PHONY: clean
clean:
	./gradlew clean

.PHONY: buildClient
buildClient:
	cd rust && cargo clean && cargo build

.PHONY: buildImage
buildImage:
	docker build -f ./Dockerfiles/$(NAME)/Dockerfile -t $(DOCKER_REG)$(DOCKER_REPO)$(NAME)$(DOCKER_TAG) \
		--build-arg CLIENT_NAME=$(CLIENT_NAME) \
		--build-arg DOCKER_REG=$(DOCKER_REG) \
		--build-arg DOCKER_REPO=$(DOCKER_REPO) \
		--build-arg DOCKER_TAG=$(DOCKER_TAG) \
		.

.PHONY: buildClientImage
buildClientImage:
	docker build -f ./Dockerfiles/$(CLIENT_NAME)/Dockerfile -t $(DOCKER_REG)$(DOCKER_REPO)$(CLIENT_NAME)$(DOCKER_TAG) .

.PHONY: buildLightclientdImage
buildLightclientdImage:
	docker build -f ./Dockerfiles/$(LIGHTCLIENTD_NAME)/Dockerfile -t $(DOCKER_REG)$(DOCKER_REPO)$(LIGHTCLIENTD_NAME)$(DOCKER_TAG) .

.PHONY: deployNodes
deployNodes:
	./gradlew deployNodes

.PHONY: upNodes
upNodes:
	./build/nodes/runnodes --headless
	for p in `seq  2222 1  2223`; do while ! nc -z localhost $$p; do sleep 1; done; done
	for p in `seq 10003 3 10006`; do while ! nc -z localhost $$p; do sleep 1; done; done

.PHONY: downNodes
downNodes:
	-sshpass -p test ssh -p 2222 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2223 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown

.PHONY: prepareHost
prepareHost:
	./gradlew :grpc-adapter:runServer --args 'localhost 10003 user1 test 9999' &
	while ! nc -z localhost 9999; do sleep 1; done
	$(CLIENT) genesis create-genesis -e http://localhost:9999 -p PartyA,Notary > base-hash.txt
	$(CLIENT) host create-host           -e http://localhost:9999 -b `cat base-hash.txt` \
		-m nop:jp.datachain.corda.ibc.ics26.NopModule \
		-m transfer-old:jp.datachain.corda.ibc.ics20.Module \
		-m transfer:jp.datachain.corda.ibc.ics20cash.Module \
		-c ibc.lightclients.fabric.v1.ClientState:jp.datachain.corda.ibc.clients.fabric.FabricClientStateFactory \
		-c ibc.lightclients.corda.v1.ClientState:jp.datachain.corda.ibc.clients.corda.CordaClientStateFactory 
	$(CLIENT) cash-bank create-cash-bank -e http://localhost:9999 -b `$(CLIENT) node address-from-name -e http://localhost:9999 -n Notary`
	$(CLIENT) cash-bank allocate-cash    -e http://localhost:9999 -o `$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA` -c USD -a 100
	$(CLIENT) admin shutdown             -e http://localhost:9999

.PHONY: runServer
runServer:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash.txt`"

.PHONY: startServer
startServer:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash.txt`" &
	while ! nc -z localhost 9999; do sleep 1; done

.PHONY: shutdownServer
shutdownServer:
	$(CLIENT) admin shutdown -e http://localhost:9999

.PHONY: startServerBank
startServerBank:
	./gradlew :grpc-adapter:runServer --args "localhost 10003 user1 test 29999 `cat base-hash.txt`" &
	while ! nc -z localhost 29999; do sleep 1; done

.PHONY: shutdownServerBank
shutdownServerBank:
	$(CLIENT) admin shutdown -e http://localhost:29999

.PHONY: upA
upA:
	docker run -d --rm --name yui-corda-ibc-a -p 9999:9999 -p 29999:29999 yui-corda-ibc:latest
	while ! wget -q -O - 'localhost:9999'; do sleep 1; done
	while ! wget -q -O - 'localhost:29999'; do sleep 1; done

.PHONY: upB
upB:
	docker run -d --rm --name yui-corda-ibc-b -p 9998:9999 -p 29998:29999 yui-corda-ibc:latest
	while ! wget -q -O - 'localhost:9998'; do sleep 1; done
	while ! wget -q -O - 'localhost:29998'; do sleep 1; done

.PHONY: downA
downA:
	docker stop yui-corda-ibc-a

.PHONY: downB
downB:
	docker stop yui-corda-ibc-b

.PHONY: executeOldTest
executeOldTest:
	./gradlew :grpc-adapter:runClient --args "executeTest localhost:9999 localhost:9998 localhost:29999 `$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA` `$(CLIENT) node address-from-name -e http://localhost:9998 -n PartyA`"

.PHONY: executeTest
executeTest:
	$(CLIENT) client create-clients
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

.PHONY: test
test: upA upB executeTest downA downB

.PHONY: oldTest
oldTest: upA upB executeOldTest downA downB

.PHONY: testContracts
testContracts:
	./gradlew :contracts:test

.PHONY: testFlows
testFlows:
	./gradlew :workflows:test

.PHONY: testRelayerBuild
testRelayerBuild:
	cd go/cmd/relayer && go build . && rm relayer

.PHONY: dumpInfos
dumpInfos:
	$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA > party-a-addr.txt
	$(CLIENT) node address-from-name -e http://localhost:9998 -n PartyA > party-b-addr.txt
	$(CLIENT) host query-host > host.txt
	$(CLIENT) cash-bank query-cash-bank > cash-bank.txt
	$(CLIENT) client query-client-state -c corda-ibc-0 -o client.dat
	$(CLIENT) client query-consensus-state -c corda-ibc-0 -n 0 -h 1 -o consensus.dat
	$(CLIENT) connection query-connection -c connection-0 -o connection.dat
	$(CLIENT) channel query-channel -c channel-0 -o channel.dat

.PHONY: proto-gen-go
proto-gen-go:
	docker run -v $(PWD):/workspace -w /workspace tendermintdev/sdk-proto-gen:v0.3 sh protogen.sh
