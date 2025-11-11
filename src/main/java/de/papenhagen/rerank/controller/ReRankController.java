package de.papenhagen.rerank.controller;


import de.papenhagen.rerank.controller.request.RankingRequest;
import de.papenhagen.rerank.dto.RankingDTO;
import de.papenhagen.rerank.mapper.RankingMapper;
import de.papenhagen.rerank.service.ReRankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReRankController {

    RankingMapper mapper;
    ReRankService service;

    public ReRankController(RankingMapper mapper, ReRankService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @PostMapping("/rerank")
    public ResponseEntity<String> rerank(final List<RankingRequest> pairs) {
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException();
        }
        List<RankingDTO> rankingDTOStream = pairs.stream()
                .map(p -> mapper.sourceToDestination(p)).toList();

        final String ranking = this.service.rerank(rankingDTOStream);
        return ResponseEntity.ok(ranking);
    }
}
