package main

import (
	"context"
	"github.com/segmentio/kafka-go"
	"log"
	"time"
)

func main() {

	topic := "villager"
	partition := 0

	conn, _ := kafka.DialLeader(context.Background(), "tcp", "localhost:19092", topic, partition)

	_ = conn.SetWriteDeadline(time.Now().Add(10 * time.Second))

	dragon :=
		kafka.Message{
			Key:   []byte("golang"),
			Value: []byte("Dragon"),
		}

	var messages [] kafka.Message

	messages = append(messages, dragon)

	_, err := conn.WriteMessages(
		messages[0],
	)
	log.Printf("Delivered %v" , string(dragon.Value))

	if (err != nil) {
		log.Printf("Encountered an error ", err)
	}

	_ = conn.Close()
}
