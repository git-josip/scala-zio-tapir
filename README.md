Here is the updated `README.md` to reflect the changes:

# Project Overview

This project is a reactive application built with Scala, ZIO, and Tapir. It includes functionalities for managing products and reviews, and integrates with Kafka for streaming product producer events.

## Features

- **Product Management**: Create, update, delete, and retrieve products.
- **Review Management**: Create, update, delete, and retrieve reviews for products.
- **Kafka Integration**: Stream product producer events using Kafka and ZIO Streams.

## Getting Started

### Prerequisites

- JDK 11 or higher
- SBT
- Docker (for running Kafka and PostgreSQL)

### Setting Up

1. **Clone the repository**:
    ```sh
    git clone https://github.com/your-repo.git
    cd your-repo
    ```

2. **Start Docker containers**:
    ```sh
    docker-compose up -d
    ```

3. **Run the application**:
    ```sh
    sbt run
    ```

### Running Tests

To run the tests, use:
```sh
sbt test
```

## Project Structure

- `src/main/scala/com/reactive/ziotapir/domain/data`: Contains domain models for `Product` and `Review`.
- `src/main/scala/com/reactive/ziotapir/http/requests`: Contains request models for creating products and reviews.
- `src/main/scala/com/reactive/ziotapir/repositories`: Contains repository implementations for `Product` and `Review`.
- `src/main/scala/com/reactive/ziotapir/services`: Contains service implementations for managing products and reviews.
- `src/main/scala/com/reactive/ziotapir/streams`: Contains Kafka producer and consumer implementations using ZIO Streams.

## Kafka Integration

The project includes a module for streaming product producer events using Kafka and ZIO Streams. Ensure Kafka is running and properly configured in `application.conf`.

### Example Kafka Producer

```scala
package com.reactive.ziotapir.streams

import zio._
import zio.kafka.producer._
import zio.kafka.serde._
import com.reactive.ziotapir.domain.data.Product

object ProductProducer {
  def produce(product: Product): RIO[Producer, Unit] = {
    val record = new ProducerRecord("products", product.id.toString, product)
    Producer.produce(record, Serde.string, Serde.json[Product])
  }
}
```

## Configuration

Configuration settings are located in `src/main/resources/application.conf`. Adjust the settings as needed for your environment.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.