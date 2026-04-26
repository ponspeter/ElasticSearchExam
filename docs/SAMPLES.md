### Sample Data & Queries

This document provides the necessary components to test the Elasticsearch Search Decomposition MVP, including the index schema, sample data, and complex queries.

> [!TIP]
> All the samples below are pre-configured and ready to use in the [Elastic.postman_collection.json](Elastic.postman_collection.json). Using Postman is the recommended way to test the application.

---

#### 1. Index Schema (Mapping)
Create an index named `products` with the following mapping:

```json
PUT /products
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
}
```

---

#### 2. Sample Test Data
Use the Bulk API to ingest sample data.

**Important:** The Bulk API requires each JSON object to be on a single line and the request must end with a newline (NDJSON format).

```bash
curl --location 'http://localhost:9200/products/_bulk' \
--header 'Content-Type: application/x-ndjson' \
--data-binary '{"index":{}}
{"name": "Sony Bravia 4K TV", "category": "Electronics", "price": 599.99, "brand": "Sony", "in_stock": true, "rating": 4.5}
{"index":{}}
{"name": "Samsung Galaxy S21", "category": "Electronics", "price": 799.00, "brand": "Samsung", "in_stock": true, "rating": 4.7}
{"index":{}}
{"name": "Logitech Mouse", "category": "Electronics", "price": 25.50, "brand": "Logitech", "in_stock": true, "rating": 4.2}
{"index":{}}
{"name": "Sony Headphones", "category": "Electronics", "price": 150.00, "brand": "Sony", "in_stock": false, "rating": 4.8}
{"index":{}}
{"name": "Generic Toaster", "category": "Home Appliances", "price": 30.00, "brand": "Generic", "in_stock": true, "rating": 3.5}
{"index":{}}
{"name": "Samsung Microwave", "category": "Home Appliances", "price": 120.00, "brand": "Samsung", "in_stock": true, "rating": 4.0}
'
```

---

#### 3. Sample Complex Queries for Decomposition

**Example 1: Electronics Price Range & Brand Filter**
This query mimics the requirement example.

`POST /api/es-decomposition/decompose?index=products`
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

**Example 2: Search with Match and Rating**
`POST /api/es-decomposition/decompose?index=products`
```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "Sony" } }
      ],
      "filter": [
        { "range": { "rating": { "gte": 4.0 } } }
      ]
    }
  }
}
```

---

#### 4. Expected Tool Output (Example 1)

| Search Component | Results After Applying |
| :--- | :--- |
| Base query (all documents) | 6 |
| Term: category = Electronics | 4 |
| Range: price {gte:50,lte:800} | 3 |
| Term: in_stock = true | 2 |
| Should clause (OR logic) | 2 |
| Final result | 2 |

#### 5. Expected Tool Output (Example 2)

| Search Component | Results After Applying |
| :--- | :--- |
| Base query (all documents) | 6 |
| Range: rating {gte:4.0} | 5 |
| Match: name = Sony | 2 |
| Final result | 2 |
