### Quick Start Guide: From Docker to Testing

This document provides a step-by-step guide to setting up the environment, running the application, and testing the Elasticsearch Search Decomposition tool.

---

#### 1. Prerequisites
*   **Docker Desktop** (installed and running)
*   **Java 21**
*   **Maven** (or use the provided `./mvnw` wrapper)
*   **Postman** (optional, for testing via the provided collection)
*   **Terminal** (PowerShell or Bash)

---

#### 2. Run Elasticsearch (Docker)
First, you need a running Elasticsearch instance. You can use the provided `Dockerfile`.

1.  **Build the Image:**
    ```powershell
    docker build -t elasticsearch-exam .
    ```

2.  **Start the Container:**
    ```powershell
    docker run -d --name elasticsearch-container -p 9200:9200 -p 9300:9300 elasticsearch-exam
    ```

3.  **Verify Elasticsearch is up:**
    Visit `http://localhost:9200` or run:
    ```powershell
    curl http://localhost:9200
    ```
    *(For more details, see [docs/RUN_DOCKER.md](RUN_DOCKER.md))*

---

#### 3. Run the Spring Boot Application
Once Elasticsearch is ready, start the decomposition tool.

1.  **Navigate to the project root.**
2.  **Run the application:**
    ```powershell
    ./mvnw spring-boot:run
    ```
    The application will start on `http://localhost:8080`.

---

#### 4. Setup Sample Data
Before testing the decomposition, you need to create an index and populate it with data.

1.  **Create the `products` index:**
    ```bash
    curl -X PUT "http://localhost:9200/products" -H "Content-Type: application/json" -d'
    {
      "mappings": {
        "properties": {
          "name": { "type": "text" },
          "category": { "type": "keyword" },
          "price": { "type": "float" },
          "brand": { "type": "keyword" },
          "in_stock": { "type": "boolean" },
          "rating": { "type": "float" }
        }
      }
    }'
    ```

2.  **Ingest Sample Data:**
    *(For the full bulk data script, see [docs/SAMPLES.md](SAMPLES.md))*

---

#### 5. Test the Search Decomposition
Now you can test the decomposition endpoint using `curl` or the provided Postman collection.

##### Option A: Using Postman (Recommended)
A Postman collection is provided to make testing easier. It includes requests for setting up the index, ingesting data, and running decomposition queries.

1.  **Open Postman.**
2.  **Import the collection:** 
    *   Click the **Import** button in Postman.
    *   Drag and drop the `docs/Elastic.postman_collection.json` file into the import window, or browse to it.
3.  **Run requests in order:**
    *   `Create Product`: Sets up the index mapping in Elasticsearch.
    *   `Get Product Index`: (Optional) Verify the index was created.
    *   `Load Product`: Ingests the sample test data using the Bulk API.
    *   `Electronics Price Range & Brand Filter`: Sends a complex query to the decomposition tool.
    *   `Search with Match and Rating`: Sends another sample query to the decomposition tool.

##### Option B: Using `curl`
If you prefer the command line, you can use `curl` as shown below.

**Sample Request:**
`POST http://localhost:8080/api/es-decomposition/decompose?index=products`

**Request Body:**
```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "category": "Electronics" } },
        { "range": { "price": { "gte": 50, "lte": 800 } } },
        { "term": { "in_stock": true } }
      ],
      "should": [
        { "term": { "brand": "Sony" } },
        { "term": { "brand": "Samsung" } }
      ],
      "minimum_should_match": 1
    }
  }
}
```

**What to expect:**
The application will return a JSON response containing a list of `search_components` and the hit count after applying each one, effectively showing how the query was narrowed down.

*(For detailed approach information, see [docs/APPROACH.md](APPROACH.md))*
