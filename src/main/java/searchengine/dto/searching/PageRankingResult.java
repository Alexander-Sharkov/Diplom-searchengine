package searchengine.dto.searching;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import searchengine.model.PageEntity;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class PageRankingResult {
    private PageEntity page;
    private Double totalRanking;
}