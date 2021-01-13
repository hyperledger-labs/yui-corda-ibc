.PHONY: build
.PHONY: deployNodes upNodes downNodes
.PHONY: startGrpcAdapter testGrpcAdapter shutdownGrpcAdapter
.PHONY: intTestGrpcAdapter

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

prepareHostA:
	./gradlew :grpc-adapter:runServer --args 'localhost 10006 user1 test 9999' &
	sleep 20
	./gradlew :grpc-adapter:runClient --args 'createGenesis localhost:9999 PartyA base-id-a.txt'
	./gradlew :grpc-adapter:runClient --args "createHost localhost:9999 `cat base-id-a.txt`"
	./gradlew :grpc-adapter:runClient --args "allocateFund localhost:9999 `cat base-id-a.txt` PartyA"
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:9999'

prepareHostB:
	./gradlew :grpc-adapter:runServer --args 'localhost 10009 user1 test 19999' &
	sleep 20
	./gradlew :grpc-adapter:runClient --args 'createGenesis localhost:19999 PartyB base-id-b.txt'
	./gradlew :grpc-adapter:runClient --args "createHost localhost:19999 `cat base-id-b.txt`"
	./gradlew :grpc-adapter:runClient --args "allocateFund localhost:19999 `cat base-id-b.txt` PartyB"
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:19999'

runServerA:
	./gradlew :grpc-adapter:runServer --args "localhost 10006 user1 test 9999 `cat base-id-a.txt`"

runServerB:
	./gradlew :grpc-adapter:runServer --args "localhost 10009 user1 test 19999 `cat base-id-b.txt`"

executeTest:
	./gradlew :grpc-adapter:runClient --args 'executeTest localhost:9999 localhost:19999'

shutdownServerA:
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:9999'

shutdownServerB:
	./gradlew :grpc-adapter:runClient --args 'shutdown localhost:19999'

test:
	make prepareHostA
	make prepareHostB
	make runServerA &
	make runServerB &
	sleep 20
	make executeTest
	make shutdownServerA
	make shutdownServerB
