# Gradle Guide

## Download dependencies
Use the Gradle Wrapper to download dependencies:

```sh
./gradlew build --refresh-dependencies
```

If you want to resolve dependencies without running tests:

```sh
./gradlew assemble --refresh-dependencies
```

## Run the job
Example with the default CSV (classpath):

```sh
./gradlew bootRun
```

To use the CSV from the classpath:

```sh
./gradlew bootRun --args="--spring.batch.job.name=importTransactionsJob inputFile=classpath:csv/transactions_2026-01-27.csv"
```
