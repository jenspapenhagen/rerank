package de.papenhagen.rerank.mapper;

import de.papenhagen.rerank.controller.request.RankingRequest;
import de.papenhagen.rerank.dto.RankingDTO;
import org.mapstruct.Mapper;

@Mapper
public interface RankingMapper {
    RankingDTO sourceToDestination(RankingRequest source);

    RankingRequest destinationToSource(RankingDTO destination);
}
