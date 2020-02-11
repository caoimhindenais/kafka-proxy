These Kafka scripts are prepared by wurstmeister. Thanks!
See https://github.com/wurstmeister/kafka-docker for alot more details

```
alias kafka='docker-compose -f docker-compose-single-broker.yml up'
alias kafkastop='docker stop kafka-docker_kafka_1 && docker stop kafka-docker_zookeeper_1 && docker system prune -f'
```

And if your local docker setup gets in a knot, its always worth trying
```
docker volume prune
docker system prune
```