package com.example.elasticsearchexam.service;

import com.example.elasticsearchexam.model.DecompositionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class QueryDecompositionServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private ObjectMapper objectMapper = new ObjectMapper();

    private QueryDecompositionService decompositionService;

    @BeforeEach
    void setUp() {
        decompositionService = new QueryDecompositionService(elasticsearchOperations, objectMapper);
    }

    @Test
    void testDecomposeQuery() throws Exception {
        // Given
        String indexName = "test-index";
        String rawQueryJson = """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "category": "Electronics" } },
                    { "range": { "price": { "gte": 50, "lte": 200 } } }
                  ],
                  "should": [
                    { "term": { "brand": "Sony" } },
                    { "term": { "brand": "Samsung" } }
                  ]
                }
              }
            }
            """;

        // Mock SearchHits
        Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregates = new HashMap<>();
        
        // Mock aggregate results
        aggregates.put("step_0_Term__category___Electronics", 
            new Aggregate(new FilterAggregate.Builder().docCount(12400).build()));
        aggregates.put("step_1_Range__price__gte_50_lte_200_", 
            new Aggregate(new FilterAggregate.Builder().docCount(4800).build()));
        aggregates.put("step_2_Should_clause__OR_logic_", 
            new Aggregate(new FilterAggregate.Builder().docCount(1200).build()));
        aggregates.put("base_query",
            new Aggregate(new FilterAggregate.Builder().docCount(50000).build()));
        
        // Use reflection or a proper way to create ElasticsearchAggregations if needed, 
        // but SearchHitsImpl takes it in constructor.
        // For simplicity in this environment, let's assume we can mock the behavior.
        
        SearchHits<Map> mockSearchHits = new SearchHitsImpl<>(50000, TotalHitsRelation.EQUAL_TO, 0.0f, null, null, null,
            new ArrayList<>(), new ElasticsearchAggregations(aggregates), null, null);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Map.class), any(IndexCoordinates.class)))
            .thenReturn(mockSearchHits);

        // When
        DecompositionResponse response = decompositionService.decomposeQuery(indexName, rawQueryJson);

        // Then
        assertNotNull(response);
        assertEquals(5, response.getComponents().size());
        
        assertEquals("Base query (all documents)", response.getComponents().get(0).getComponent());
        assertEquals(50000, response.getComponents().get(0).getResultsAfterApplying());
        
        assertEquals("Term: category = Electronics", response.getComponents().get(1).getComponent());
        assertEquals(12400, response.getComponents().get(1).getResultsAfterApplying());
        
        assertEquals("Range: price {gte:50,lte:200}", response.getComponents().get(2).getComponent());
        assertEquals(4800, response.getComponents().get(2).getResultsAfterApplying());

        assertEquals("Should clause (OR logic)", response.getComponents().get(3).getComponent());
        assertEquals(1200, response.getComponents().get(3).getResultsAfterApplying());

        assertEquals("Final result", response.getComponents().get(4).getComponent());
        assertEquals(1200, response.getComponents().get(4).getResultsAfterApplying());
        
        // Verify that NativeQuery was built correctly
        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(Map.class), any(IndexCoordinates.class));
        
        NativeQuery capturedQuery = queryCaptor.getValue();
        assertNotNull(capturedQuery.getAggregations());
        assertEquals(4, capturedQuery.getAggregations().size()); // base_query + 3 steps
    }
}
