# Elasticsearch Search Decomposition MVP

A Spring Boot application that decomposes complex Elasticsearch queries to show the progressive impact of each filter on the total result count. This tool is designed to help developers and analysts understand how their search criteria narrow down results.

## Documentation Guide

To help you get started and understand the system, the following documentation is available in the `docs/` directory:

| Document | Purpose | Usage |
| :--- | :--- | :--- |
| [**GETTING_STARTED.md**](docs/GETTING_STARTED.md) | **Setup & Quick Start** | Follow this first to set up your environment (Docker, Java) and run your first decomposition test. |
| [**APPROACH.md**](docs/APPROACH.md) | **Technical Design** | Explains the "Aggregations with Filters" strategy, why it was chosen, and how the query decomposition logic works internally. |
| [**SAMPLES.md**](docs/SAMPLES.md) | **Test Data & Queries** | Contains the Elasticsearch mappings, sample product data, and example JSON queries for testing the API. |
| [**RUN_DOCKER.md**](docs/RUN_DOCKER.md) | **Environment Setup** | Detailed instructions on building and running the Elasticsearch container using the provided Dockerfile. |
| [**Elastic.postman_collection.json**](docs/Elastic.postman_collection.json) | **Testing Tool** | A ready-to-use Postman collection that includes all steps from index creation to complex query testing. |

## Recommended Roadmap

If you are new to this project, we recommend following this order:

1.  **Preparation**: Read [**RUN_DOCKER.md**](docs/RUN_DOCKER.md) to set up your Elasticsearch instance.
2.  **Execution**: Follow [**GETTING_STARTED.md**](docs/GETTING_STARTED.md) to launch the Spring Boot application.
3.  **Testing**: Use the [**Postman Collection**](docs/Elastic.postman_collection.json) along with [**SAMPLES.md**](docs/SAMPLES.md) to run test queries and see the tool in action.
4.  **Deep Dive**: Read [**APPROACH.md**](docs/APPROACH.md) to understand the underlying implementation and logic.

---

## 🛠 Prerequisites
*   Java 21
*   Docker Desktop
*   Maven (or `./mvnw` wrapper)
