#!/usr/bin/env bash
pkill -9 -f java-consumer/target/java-consumer-0.0.1-SNAPSHOT.jar  2>/dev/null &
pkill -9 -f io.proxy.KafkaProxy  2>/dev/null &
pkill -9 -f  /Library/Java/JavaVirtualMachines/graalvm-ce-java11-19.3.0/Contents/Home/bin/java  2>/dev/null &
killall bash ./go-producer.sh  2>/dev/null &
killall bash ./donet-producer.sh  2>/dev/null &
rm *.log 2>/dev/null &
rm *.out 2>/dev/null &
