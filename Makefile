.PHONY: build
.PHONY: deployNodes upNodes downNodes
.PHONY: _testGrpcAdapter testGrpcAdapter

build:
	./gradlew clean build

deployNodes:
	./gradlew deployNodes

upNodes:
	./build/nodes/runnodes --headless
	sleep 40

downNodes:
	-ssh -p 2222 user1@localhost run gracefulShutdown
	-ssh -p 2223 user1@localhost run gracefulShutdown
	-ssh -p 2224 user1@localhost run gracefulShutdown

_testGrpcAdapter:
	./gradlew :grpc-adapter:runGrpcAdapter

testGrpcAdapter: upNodes _testGrpcAdapter downNodes
