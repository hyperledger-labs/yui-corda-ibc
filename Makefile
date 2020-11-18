.PHONY: build
.PHONY: deployNodes upNodes downNodes
.PHONY: _testGrpcAdapter testGrpcAdapter

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

_testGrpcAdapter:
	./gradlew :grpc-adapter:runGrpcAdapter

testGrpcAdapter: upNodes _testGrpcAdapter downNodes
