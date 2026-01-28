# Spring Batch Playground

## Requirements
- Java 25
- Docker (for Postgres)

## Database (Docker Compose)
Start Postgres locally:

```sh
docker compose up -d
```

## Gradle
See [GRADLE_README.md](GRADLE_README.md) for Gradle commands (dependencies, running jobs).

## Job Examples

### Import Transactions Job
Imports transactions from CSV files into a database.
![importTransactionsJob](src/main/resources/diagrams/importTransactionsJob.png)
Run the job
```sh
./gradlew bootRun --args="--spring.batch.job.name=importTransactionsJob inputFile=classpath:csv/transactions_2026-01-27.csv"
```

### Enrich Customer Address Job
Enriches pending customer addresses via an external ZIP code lookup service.
![enrichCustomerAddressJob](src/main/resources/diagrams/enrich-customer-address-job.png)
Run the job
```sh
./gradlew bootRun --args="--spring.batch.job.name=enrichCustomerAddressJob"
```
