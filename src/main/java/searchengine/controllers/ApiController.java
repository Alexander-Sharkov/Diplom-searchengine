package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.BasicResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public BasicResponse startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public BasicResponse stopIndexing() {
        return indexingService.stopIndexSite();
    }

    @PostMapping("/indexPage")
    public BasicResponse indexPage(@RequestParam(name = "url") String url) {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    public BasicResponse search(@RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "site", required = false) String site,
                                @RequestParam(name = "offset", defaultValue = "0") int offset,
                                @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return searchService.search(query, site, offset, limit);
    }
}
