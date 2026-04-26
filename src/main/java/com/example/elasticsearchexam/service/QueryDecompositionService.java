package com.example.elasticsearchexam.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.elasticsearchexam.model.DecompositionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryDecompositionService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    /**
     * Decomposes an Elasticsearch query and shows the impact of each filter.
     * 
     * @param indexName The index to search against.
     * @param rawQueryJson The raw Elasticsearch query JSON (the "query" part).
     * @return DecompositionResponse containing the step-by-step result count.
     */
    public DecompositionResponse decomposeQuery(String indexName, String rawQueryJson) throws Exception {
        JsonNode rootNode = objectMapper.readTree(rawQueryJson);
        JsonNode queryNode = rootNode.has("query") ? rootNode.get("query") : rootNode;

        List<DecomposedFilter> filters = extractFilters(queryNode);
        
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchAll().build()._toQuery());
        queryBuilder.withMaxResults(0); // Only need counts

        // Progressive filtering using filter aggregations
        Map<String, Aggregation> aggregations = new LinkedHashMap<>();
        
        // Step 0: Total count (base query)
        // Handled by the search response hit count itself, or we can use a match_all filter aggregation

        List<Query> progressiveQueries = new ArrayList<>();
        
        for (int i = 0; i < filters.size(); i++) {
            DecomposedFilter filter = filters.get(i);
            progressiveQueries.add(filter.esQuery());
            
            // Create a bool query that combines all filters up to this point
            Query combinedQuery =
                BoolQuery.of(b -> b.filter(new ArrayList<>(progressiveQueries)))._toQuery();
            
            String aggName = "step_" + i + "_" + filter.description().replaceAll("[^a-zA-Z0-9]", "_");
            aggregations.put(aggName, Aggregation.of(a -> a.filter(combinedQuery)));
        }

        queryBuilder.withAggregation("base_query", Aggregation.of(a -> a.filter(QueryBuilders.matchAll().build()._toQuery())));
        aggregations.forEach(queryBuilder::withAggregation);

        NativeQuery nativeQuery = queryBuilder.build();
        SearchHits<Map> searchHits = elasticsearchOperations.search(nativeQuery, Map.class, 
            IndexCoordinates.of(indexName));

        List<DecompositionResponse.SearchComponentResult> results = new ArrayList<>();
        
        // Base count
        results.add(new DecompositionResponse.SearchComponentResult("Base query (all documents)", searchHits.getTotalHits()));

        // Step counts from aggregations
        if (searchHits.hasAggregations()) {
            ElasticsearchAggregations aggregationsContainer =
                (ElasticsearchAggregations) searchHits.getAggregations();
            
            for (int i = 0; i < filters.size(); i++) {
                DecomposedFilter filter = filters.get(i);
                String aggName = "step_" + i + "_" + filter.description().replaceAll("[^a-zA-Z0-9]", "_");
                log.info("Accessing aggregation: {}", aggName);
                
                var elasticsearchAggregation = aggregationsContainer.get(aggName);
                if (elasticsearchAggregation != null) {
                    long count = elasticsearchAggregation.aggregation().getAggregate().filter().docCount();
                    results.add(new DecompositionResponse.SearchComponentResult(filter.description(), count));
                } else {
                    log.warn("Aggregation {} not found in results", aggName);
                    results.add(new DecompositionResponse.SearchComponentResult(filter.description(), 0));
                }
            }
        }

        // Final result matches the last step
        if (!results.isEmpty()) {
            results.add(new DecompositionResponse.SearchComponentResult("Final result", results.get(results.size() - 1).getResultsAfterApplying()));
        }

        return new DecompositionResponse(results);
    }

    private List<DecomposedFilter> extractFilters(JsonNode queryNode) {
        List<DecomposedFilter> filters = new ArrayList<>();
        
        if (queryNode.has("bool")) {
            JsonNode boolNode = queryNode.get("bool");
            // Check for filter, must, should
            if (boolNode.has("filter")) {
                addFilters(boolNode.get("filter"), filters);
            }
            if (boolNode.has("must")) {
                addFilters(boolNode.get("must"), filters);
            }
            if (boolNode.has("should")) {
                // We treat the whole should block as one component to decompose
                filters.add(new DecomposedFilter(describeFilter(queryNode), convertToEsQuery(queryNode)));
            }
            // For MVP, we treat should as a single block if it's there, 
            // but complex should decomposition might be out of scope or requires special handling.
        } else {
            // It might be a single filter/query
            filters.add(new DecomposedFilter(queryNode.toString(), convertToEsQuery(queryNode)));
        }
        
        return filters;
    }

    private void addFilters(JsonNode node, List<DecomposedFilter> filters) {
        if (node.isArray()) {
            for (JsonNode filterNode : node) {
                filters.add(new DecomposedFilter(describeFilter(filterNode), convertToEsQuery(filterNode)));
            }
        } else if (node.has("should") && node.get("should").isArray()) {
            // Special case for a single bool node with should that was passed here
            filters.add(new DecomposedFilter(describeFilter(node), convertToEsQuery(node)));
        } else {
            filters.add(new DecomposedFilter(describeFilter(node), convertToEsQuery(node)));
        }
    }

    private String describeFilter(JsonNode node) {
        if (node.has("term")) {
            Map.Entry<String, JsonNode> field = node.get("term").fields().next();
            return "Term: " + field.getKey() + " = " + field.getValue().asText();
        } else if (node.has("match")) {
            Map.Entry<String, JsonNode> field = node.get("match").fields().next();
            return "Match: " + field.getKey() + " = " + field.getValue().asText();
        } else if (node.has("range")) {
            Map.Entry<String, JsonNode> field = node.get("range").fields().next();
            return "Range: " + field.getKey() + " " + field.getValue().toString().replace("\"", "");
        } else if (node.has("bool") && node.get("bool").has("should")) {
            return "Should clause (OR logic)";
        }
        // Fallback to query type or string representation
        return node.fieldNames().next() + " query";
    }

    private Query convertToEsQuery(JsonNode node) {
        try {
            String json = node.toString();
            JsonProvider provider = JsonProvider.provider();
            JsonParser parser = provider.createParser(new java.io.StringReader(json));
            return Query._DESERIALIZER.deserialize(parser, new JacksonJsonpMapper(objectMapper));
        } catch (Exception e) {
            log.error("Failed to parse filter query: {}", node.toString(), e);
            return QueryBuilders.matchAll().build()._toQuery();
        }
    }

    private record DecomposedFilter(String description, Query esQuery) {
    }
}
