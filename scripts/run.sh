#!/usr/bin/env bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11.0.5) && export PATH=$JAVA_HOME/bin:$PATH
mvn clean package -f ./kafka-netty-proxy/pom.xml
./kafka-netty-proxy/target/io.proxy.kafkaproxy 1>> kafka-proxy.out 2>&1 &

export JAVA_HOME=$(/usr/libexec/java_home -v 11.0.3) && export PATH=$JAVA_HOME/bin:$PATH
mvn clean install -f ./java-consumer/pom.xml -DskipTests
java -jar java-consumer/target/java-consumer-0.0.1-SNAPSHOT.jar >> java-consumer.log &

cd ./donet-producer/ && dotnet publish && cd ..
cd ./go-producer/ && go build && cd ..

while sleep 5; do
    ./go-producer/go-producer 1>> go-producer.log 2>&1
done &

while sleep 2; do
    dotnet run --project donet-producer/ >>  donet-producer.log
done &
