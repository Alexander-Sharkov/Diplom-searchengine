package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.StatusEnum;
import searchengine.repositories.SiteRepository;
import searchengine.services.StatisticsService;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;

    private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();

        sitesList.forEach(s -> {
            SiteEntity site = siteRepository.findByUrl(s.getUrl());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(s.getName());
            item.setUrl(s.getUrl());

            int pages = 0;
            int lemmas = 0;
            String status = StatusEnum.FAILED.name();
            long statusTime = 0;
            String error = "";

            if (site != null) {
                pages = site.getPages().size();
                lemmas = site.getLemmas().size();
                status = site.getStatus().name();
                statusTime = site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                error = site.getLastError() == null ? "" : site.getLastError();
            }

            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(status);
            item.setError(error);
            item.setStatusTime(statusTime);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        });

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}