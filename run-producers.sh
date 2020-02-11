#!/usr/bin/env bash
while sleep 5; do
    ./go-producer/go-producer 1>> go-producer.log 2>&1
done &

while sleep 2; do
    dotnet run --project donet-producer/ >>  donet-producer.log
done &