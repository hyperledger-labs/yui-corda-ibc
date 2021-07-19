.PHONY: build
.PHONY: deployNodes upNodes downNodes
.PHONY: prepareHostA startServerA shutdownServerA startServerBankA shutdownServerBankA
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
	./gradlew :grpc-adapter:runServer --args 'localhost 10012 user1 test 9999' &
	sleep 20
	$(CLIENT) genesis create-genesis -e http://localhost:9999 -p PartyA,Bank,Notary > base-hash-a.txt
	$(CLIENT) admin shutdown         -e http://localhost:9999
	./gradlew :grpc-adapter:runServer --args "localhost 10012 user1 test 9999 `cat base-hash-a.txt`" &
	sleep 20
	$(CLIENT) host create-host           -e http://localhost:9999
	$(CLIENT) cash-bank create-cash-bank -e http://localhost:9999 -b `$(CLIENT) node address-from-name -e http://localhost:9999 -n Bank`
	$(CLIENT) cash-bank allocate-cash    -e http://localhost:9999 -o `$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA` -c USD -a 100
	$(CLIENT) admin shutdown             -e http://localhost:9999

prepareHostB:
	./gradlew :grpc-adapter:runServer --args 'localhost 10012 user1 test 19999' &
	sleep 20
	$(CLIENT) genesis create-genesis -e http://localhost:19999 -p PartyB,Bank,Notary > base-hash-b.txt
	$(CLIENT) admin shutdown         -e http://localhost:19999
	./gradlew :grpc-adapter:runServer --args "localhost 10012 user1 test 19999 `cat base-hash-b.txt`" &
	sleep 20
	$(CLIENT) host create-host           -e http://localhost:19999
	$(CLIENT) cash-bank create-cash-bank -e http://localhost:19999 -b `$(CLIENT) node address-from-name -e http://localhost:19999 -n Bank`
	$(CLIENT) admin shutdown             -e http://localhost:19999

runServerA:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash-a.txt`"

startServerA:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash-a.txt`" &
	sleep 10

startServerB:
	./gradlew :grpc-adapter:runServer --args "localhost 10009 user1 test 19999 `cat base-hash-b.txt`" &
	sleep 10

startServerBankA:
	./gradlew :grpc-adapter:runServer --args "localhost 10012 user1 test 29999 `cat base-hash-a.txt`" &
	sleep 10

executeOldTest:
	./gradlew :grpc-adapter:runClient --args "executeTest localhost:9999 localhost:19999 localhost:29999 `$(CLIENT) node address-from-name -e http://localhost:9999 -n PartyA` `$(CLIENT) node address-from-name -e http://localhost:19999 -n PartyB`"

executeTest:
	$(CLIENT) client create-clients \
		--client-id-a corda-0 \
		--client-id-b corda-0
	$(CLIENT) connection handshake \
		--client-id-a corda-0 \
		--client-id-b corda-0 \
		--connection-id-a connection-0 \
		--connection-id-b connection-0
	$(CLIENT) channel handshake \
		--connection-id-a connection-0 \
		--connection-id-b connection-0 \
		--channel-id-a channel-0 \
		--channel-id-b channel-0

shutdownServerA:
	$(CLIENT) admin shutdown -e http://localhost:9999

shutdownServerB:
	$(CLIENT) admin shutdown -e http://localhost:19999

shutdownServerBankA:
	$(CLIENT) admin shutdown -e http://localhost:29999

test: prepareHostA prepareHostB startServerA startServerB executeTest shutdownServerA shutdownServerB

oldTest: prepareHostA prepareHostB startServerA startServerB startServerBankA executeOldTest shutdownServerA shutdownServerB shutdownServerBankA
