# Scala ZIO Tapir Project

This project is a Scala-based web application using ZIO, Tapir, and Quill for building reactive and type-safe APIs. The project includes a service layer, repository layer, and HTTP controllers for managing `Company` and `Review` entities.

## Project Structure

- `src/main/scala/com/reactive/ziotapir/domain/data`: Contains domain models such as `Company` and `Review`.
- `src/main/scala/com/reactive/ziotapir/http/controllers`: Contains HTTP controllers for handling API requests.
- `src/main/scala/com/reactive/ziotapir/repositories`: Contains repository interfaces and implementations for database operations.
- `src/main/scala/com/reactive/ziotapir/services`: Contains service layer implementations for business logic.
- `src/test/scala/com/reactive/ziotapir/http/controllers`: Contains test cases for HTTP controllers.

## Dependencies

The project uses the following dependencies:

- ZIO
- Tapir
- Quill
- PostgreSQL
- Flyway
- ZIO Test
- ZIO Mock
- ZIO Config
- Logback
- Java JWT
- Stripe Java

## Getting Started

### Building the Project

To build the project, run:

```sh
sbt compile
```

### Running the Application

To run the application, use:

```sh
sbt run
```

### Running Tests

To run the tests, use:

```sh
sbt test
```

## Configuration

Configuration is managed using ZIO Config. Update the configuration files as needed to match your environment.


## API Endpoints

### Company Endpoints

- `POST /companies`: Create a new company.
- `GET /companies`: Get all companies.
- `GET /companies/{id}`: Get a company by ID.

### Review Endpoints

- `POST /reviews`: Create a new review.
- `GET /reviews`: Get all reviews.
- `GET /reviews/{id}`: Get a review by ID.

## Error Handling

Custom error handling is implemented using Tapir. Validation errors are returned as JSON responses with appropriate error messages.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Acknowledgements

- [ZIO](https://zio.dev/)
- [Tapir](https://tapir.softwaremill.com/)
- [Quill](https://getquill.io/)

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.