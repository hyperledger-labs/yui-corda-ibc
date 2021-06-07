.PHONY: build
.PHONY: deployNodes upNodes downNodes
.PHONY: prepareHostA startServerA shutdownServerA
.PHONY: prepareHostB startServerB shutdownServerB
.PHONY: executeTest executeOldTest
.PHONY: test oldTest

CLIENT ?= ./rust/target/release/corda-ibc-client

build:
	./gradlew -x test clean build

deployNodes:
	./gradlew deployNodes

upNodes:
	./build/nodes/runnodes --headless
	sleep 60

downNodes:
	-sshpass -p test ssh -p 2222 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2223 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2224 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2225 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown

killNodes:
	kill -9 $$(jps -l | grep 'main class information unavailable' | cut -d ' ' -f 1)

prepareHostA:
	./gradlew :grpc-adapter:runServer --args 'localhost 10006 user1 test 9999' &
	sleep 20
	$(CLIENT) genesis create-genesis -e http://localhost:9999 -p PartyA > base-hash-a.txt
	$(CLIENT) admin shutdown         -e http://localhost:9999
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash-a.txt`" &
	sleep 20
	$(CLIENT) host create-host   -e http://localhost:9999
	$(CLIENT) bank create-bank   -e http://localhost:9999
	$(CLIENT) bank allocate-fund -e http://localhost:9999 -p cosmos1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqnrql8a -d USD -a 100
	$(CLIENT) admin shutdown     -e http://localhost:9999

prepareHostB:
	./gradlew :grpc-adapter:runServer --args 'localhost 10009 user1 test 19999' &
	sleep 20
	$(CLIENT) genesis create-genesis -e http://localhost:19999 -p PartyB > base-hash-b.txt
	$(CLIENT) admin shutdown         -e http://localhost:19999
	./gradlew :grpc-adapter:runServer --args "localhost 10009 user1 test 19999 `cat base-hash-b.txt`" &
	sleep 20
	$(CLIENT) host create-host -e http://localhost:19999
	$(CLIENT) bank create-bank -e http://localhost:19999
	$(CLIENT) admin shutdown   -e http://localhost:19999

allocateForRelayerTest:
	$(CLIENT) bank allocate-fund -e http://localhost:9999 -p cosmos1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqnrql8a -d USD -a 100

runServerA:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash-a.txt`"

startServerA:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash-a.txt`" &
	sleep 10

startServerB:
	./gradlew :grpc-adapter:runServer --args "localhost 10009 user1 test 19999 `cat base-hash-b.txt`" &
	sleep 10

executeOldTest:
	./gradlew :grpc-adapter:runClient --args "executeTest localhost:9999 localhost:19999 cosmos1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqnrql8a cosmos1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqnrql8a"

executeTest:
	$(CLIENT) client create-clients \
		--client-id-a aliceclient \
		--client-id-b bobclient
	$(CLIENT) connection handshake \
		--client-id-a aliceclient \
		--client-id-b bobclient \
		--connection-id-a aliceconnection \
		--connection-id-b bobconnection
	$(CLIENT) channel handshake \
		--connection-id-a aliceconnection \
		--connection-id-b bobconnection \
		--channel-id-a alicechannel \
		--channel-id-b bobchannel

shutdownServerA:
	$(CLIENT) admin shutdown -e http://localhost:9999

shutdownServerB:
	$(CLIENT) admin shutdown -e http://localhost:19999

test: prepareHostA prepareHostB startServerA startServerB executeTest shutdownServerA shutdownServerB

oldTest: prepareHostA prepareHostB startServerA startServerB executeOldTest shutdownServerA shutdownServerB
