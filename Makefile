.PHONY: build
.PHONY: deployNodes upNodes downNodes
.PHONY: prepareHostA startServerA shutdownServerA
.PHONY: prepareHostB startServerB shutdownServerB
.PHONY: executeTest
.PHONY: test

build:
	./gradlew -x test clean build

deployNodes:
	./gradlew deployNodes

upNodes:
	./build/nodes/runnodes --headless
	sleep 40

downNodes:
	-sshpass -p test ssh -p 2222 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2223 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown
	-sshpass -p test ssh -p 2224 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no user1@localhost run gracefulShutdown

killNodes:
	kill -9 $$(jps -l | grep 'main class information unavailable' | cut -d ' ' -f 1)

prepareHostA:
	./gradlew :grpc-adapter:runServer --args 'localhost 10006 user1 test 9999' &
	sleep 20
	./gradlew :grpc-adapter:runClient --args 'createGenesis localhost:9999 PartyA base-hash-a.txt'
	./gradlew :grpc-adapter:runClient --args "createHost localhost:9999 `cat base-hash-a.txt`"
	./gradlew :grpc-adapter:runClient --args "allocateFund localhost:9999 `cat base-hash-a.txt` PartyA"
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:9999'

prepareHostB:
	./gradlew :grpc-adapter:runServer --args 'localhost 10009 user1 test 19999' &
	sleep 20
	./gradlew :grpc-adapter:runClient --args 'createGenesis localhost:19999 PartyB base-hash-b.txt'
	./gradlew :grpc-adapter:runClient --args "createHost localhost:19999 `cat base-hash-b.txt`"
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:19999'

allocateForRelayerTest:
	./gradlew :grpc-adapter:runClient --args "allocateFund localhost:9999 `cat base-hash-a.txt` cosmos1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqnrql8a"

startServerA:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-hash-a.txt`" &
	sleep 10

startServerB:
	./gradlew :grpc-adapter:runServer --args "localhost 10009 user1 test 19999 `cat base-hash-b.txt`" &
	sleep 10

executeTest:
	./gradlew :grpc-adapter:runClient --args 'executeTest localhost:9999 localhost:19999 PartyA PartyB'

shutdownServerA:
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:9999'

shutdownServerB:
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:19999'

test: prepareHostA prepareHostB startServerA startServerB executeTest shutdownServerA shutdownServerB
