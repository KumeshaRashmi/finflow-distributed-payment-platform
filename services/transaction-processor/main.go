package main

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/segmentio/kafka-go"
)

type HealthResponse struct {
	Status  string `json:"status"`
	Service string `json:"service"`
}

type PaymentEvent struct {
	EventID             string `json:"eventId"`
	PaymentID           string `json:"paymentId"`
	SourceAccountID     int64  `json:"sourceAccountId"`
	DestinationAccountID int64 `json:"destinationAccountId"`
	Amount              string `json:"amount"`
	Currency            string `json:"currency"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(HealthResponse{
		Status:  "UP",
		Service: "transaction-processor",
	})
}

func consumePayments(ctx context.Context) {
	broker := os.Getenv("KAFKA_BROKER")
	if broker == "" {
		broker = "localhost:9092"
	}

	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:        []string{broker},
		Topic:          "payment-requests",
		GroupID:        "finflow-transaction-processor",
		MinBytes:       1,
		MaxBytes:       10e6,
		CommitInterval: 0,
	})
	defer reader.Close()

	log.Println("Waiting for payment events from Kafka")

	for {
		message, err := reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil || errors.Is(err, context.Canceled) {
				return
			}
			log.Printf("Kafka read error: %v", err)
			time.Sleep(time.Second)
			continue
		}

		var event PaymentEvent
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("Invalid JSON in Kafka message: %v", err)
			continue
		}

		if event.EventID == "" ||
			event.PaymentID == "" ||
			event.SourceAccountID <= 0 ||
			event.DestinationAccountID <= 0 ||
			event.SourceAccountID == event.DestinationAccountID ||
			event.Amount == "" ||
			event.Currency == "" {
			log.Printf("Invalid payment event: %s", message.Value)
			continue
		}

		// Connectivity test only: this event has NOT been processed.
		// Do not commit it or mark the payment COMPLETED yet.
		log.Printf(
			"Received event=%s payment=%s amount=%s %s",
			event.EventID,
			event.PaymentID,
			event.Amount,
			event.Currency,
		)
	}
}

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	go consumePayments(ctx)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8084"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", healthHandler)

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
	}

	log.Printf("Transaction Processor listening on port %s", port)

	if err := server.ListenAndServe(); err != nil &&
		err != http.ErrServerClosed {
		log.Fatal(err)
	}
}