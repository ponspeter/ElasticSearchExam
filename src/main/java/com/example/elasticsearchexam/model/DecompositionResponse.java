package com.example.elasticsearchexam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecompositionResponse {
    private List<SearchComponentResult> components;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchComponentResult {
        private String component;
        private long resultsAfterApplying;
    }
}
