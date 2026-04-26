package com.example.elasticsearchexam.controller;

import com.example.elasticsearchexam.model.DecompositionResponse;
import com.example.elasticsearchexam.service.QueryDecompositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/es-decomposition")
@RequiredArgsConstructor
public class DecompositionController {

    private final QueryDecompositionService decompositionService;

    @PostMapping("/decompose")
    public DecompositionResponse decompose(@RequestParam String index, @RequestBody String queryJson) throws Exception {
        return decompositionService.decomposeQuery(index, queryJson);
    }
}
