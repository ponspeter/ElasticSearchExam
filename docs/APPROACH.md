### Elasticsearch Search Decomposition MVP

#### Chosen Approach: Option B – Aggregations with Filters

For the Search Decomposition MVP, I have implemented **Option B: Aggregations with Filters**. This approach leverages Elasticsearch's powerful aggregation framework to compute the progressive impact of filters in a single request.

---

#### 1. Justification of the Choice

| Feature | Option A: Multiple Queries | Option B: Aggregations with Filters (Chosen) |
| :--- | :--- | :--- |
| **Efficiency** | High overhead. Requires $N+1$ round-trips for $N$ filters. | **Optimized**. Uses a single round-trip to Elasticsearch. |
| **Performance** | Slower due to network latency and repeated query parsing. | **Faster**. ES processes all filter steps in a single execution context. |
| **Consistency** | Risk of data change between requests (in high-velocity indices). | **Consistent**. All counts are calculated against the same point-in-time snapshot. |
| **Complexity** | Simple logic but hard to scale. | Slightly higher complexity in query construction, but much more robust. |

**Conclusion:** Option B is the industry standard for faceted search and search analytics. It minimizes network overhead and maximizes the use of Elasticsearch’s internal caching mechanisms. I selected Option B because it ensures that progressive filter impacts are computed within a single execution context, drastically reducing latency compared to sequential queries.

---

#### 2. Technical Implementation Details

**A. Query Decomposition**
The tool parses the incoming `bool` query. It specifically extracts filters from the `filter` and `must` clauses. Each extracted filter is treated as a logical unit for decomposition.

**B. Progressive Aggregation Building**
Instead of just running the filters, the tool constructs a "Progressive Filter Chain":
1.  **Step 0**: `Match All` (Base Count).
2.  **Step 1**: `Filter A`.
3.  **Step 2**: `Filter A` AND `Filter B`.
4.  **Step 3**: `Filter A` AND `Filter B` AND `Filter C`.

This is achieved by creating a `filter` aggregation for each step. Each aggregation contains a `bool` query that includes all preceding filters plus the current one.

**C. Result Extraction**
The tool executes one `NativeQuery`. It then traverses the `ElasticsearchAggregations` container in the response, mapping each named aggregation back to its original filter description to build the final result table.

---

#### 3. Handling Requirements & Edge Cases

*   **Ordering**: Filters are processed in the exact order they appear in the original JSON array (`must` or `filter` blocks), ensuring the "narrowing down" experience follows the user's query structure.
*   **Query Types**: The system supports `term`, `match`, and `range` queries with human-readable descriptions. Other query types are identified by their name (e.g., "exists query").
*   **Efficiency**: By setting `withMaxResults(0)`, we tell Elasticsearch we only care about the hit counts (aggregations), not the actual documents, further improving performance.
*   **Booleans/Should**: For the MVP, `should` clauses are treated as single atomic blocks to avoid combinatorial explosion, though the architecture allows for deeper recursive decomposition in future iterations.

---

#### 4. Assumptions & Limitations

*   **Filter Context**: The tool assumes the primary goal is counting hits. It converts all components into a filter context (no scoring impact).
*   **Query Structure**: It expects a standard Elasticsearch Query DSL structure (primarily `bool` based).
---

#### 5. Sample Data & Testing
For a complete guide on how to test this tool, including an index schema, bulk test data, and sample complex queries, please refer to the [SAMPLES.md](SAMPLES.md) file.
