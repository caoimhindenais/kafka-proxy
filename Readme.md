# Kafka Proxy

An example of a Kafka Proxy built using netty.  See references below for related work.

<p align='right'>A <a href="http://www.swisspush.org">swisspush</a> project <a href="http://www.swisspush.org" border=0><img align="top"  src='https://1.gravatar.com/avatar/cf7292487846085732baf808def5685a?s=32'></a></p>

__User Story__

Two kafka producers (golang and dotnet) are producing messages and a springboot application is 
consuming them. The message content should be encrypted (not the key's) and this should be transparent to all Kafka clients.

![Alt Text](./kafka.gif)

__Quickstart__

_Note : adjust JAVA_HOME's accordingly to your local deployment's_ 

1. Start the Kafka broker
    ```
    docker-compose -f broker/docker-compose-single-broker.yml up
    ```

2. Build and run Kafka proxy using __OpenJDK 64-Bit GraalVM CE 19.3.0__ 
    ```
    export JAVA_HOME=$(/usr/libexec/java_home -v 11.0.5) && export PATH=$JAVA_HOME/bin:$PATH
    mvn clean package -f ./kafka-netty-proxy/pom.xml
    ./kafka-netty-proxy/target/io.proxy.kafkaproxy 1>> kafka-proxy.out 2>&1 &
    ```

3. Build and run the Spring boot Kafka consumer using __OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.3+7)__
    ```
    export JAVA_HOME=$(/usr/libexec/java_home -v 11.0.3) && export PATH=$JAVA_HOME/bin:$PATH
    mvn clean install -f ./java-consumer/pom.xml -DskipTests
    java -jar java-consumer/target/java-consumer-0.0.1-SNAPSHOT.jar >> java-consumer.log &
    ```
_Note : Sometimes on startup the consumer throws a "Cannot connect to empty node :19092" simply restart the consumer (or send a pull request :-)) . Afterwards it's fine.  I'm thinking it's some race condition _ 



4. Build and start the producer's
    ```
    cd ./donet-producer/ && dotnet publish && cd ..
    cd ./go-producer/ && go build && cd ..
    
    while sleep 5; do
        ./go-producer/go-producer 1>> go-producer.log 2>&1
    done &
    
    while sleep 2; do
        dotnet run --project donet-producer/ >>  donet-producer.log
    done &
    ```
_Note : 
  
__Resources__
 
 There has been some work by different people in the past few year's and this is not an exhaustive list but 
 heres what I've stumbed accross. David Jacot [ [1](https://www.confluent.io/kafka-summit-lon19/handling-gdpr-apache-kafka-comply-freaking-out/) ] gave a really interesting talk at the Bern Streaming Meetup 
 group demoing how you might implement GDPR compliance using Kafka Proxies. Prior to that Travis Jeffery 
 wrote an implementation of Kafka and a Proxy in golang [ [2](https://www.confluent.io/kafka-summit-lon19/handling-gdpr-apache-kafka-comply-freaking-out/) ]. Banzaicloud took a slightly different 
 angle [ [3](https://banzaicloud.com/blog/kafka-envoy-protocol-filter/) ] and  ran a Kafka cluster on Istio. And finally today Adam Kotwasinski is busy implementing a 
 Kafka filter for envoy  [ [4](https://github.com/envoyproxy/envoy/issues/2852) ]
 
 [1] https://www.confluent.io/kafka-summit-lon19/handling-gdpr-apache-kafka-comply-freaking-out/ <br/>
 [2] https://github.com/travisjeffery/kafka-proxy  <br/>
 [3] https://banzaicloud.com/blog/kafka-envoy-protocol-filter/  <br/>
 [4] https://github.com/envoyproxy/envoy/issues/2852
 
 
 __Prerequisites__
 
 [5]: https://dotnet.microsoft.com/download <br/>
 [6]: https://golang.org/doc/install <br/>
 [7]: https://adoptopenjdk.net <br/>
 [8]: https://github.com/graalvm/graalvm-ce-builds/releases 
