package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.dto.BasicResponse;
import searchengine.dto.ErrorResponse;
import searchengine.dto.searching.PageRankingResult;
import searchengine.dto.searching.SearchDataItem;
import searchengine.dto.searching.SearchResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusEnum;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Lemmatizer lemmatizer;
    private final JsoupConfig jsoupConfig;

    @Cacheable(value = "searchResults", key = "{ #query, #siteUrl }")
    public BasicResponse search(String query, String siteUrl, int offset, int limit) {
        log.info("Выполнение поискового запроса: {}, siteUrl: {}, offset: {}, limit: {}", query, siteUrl, offset, limit);
        if (query.isBlank()) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }

        SiteEntity site = null;

        if (siteUrl != null) {
            site = siteRepository.findByUrl(siteUrl);
        }

        if (sitesNotIndexing(site)) {
            return new ErrorResponse("Сайт(ы) не проиндексирован(ы)");
        }

        List<SearchDataItem> dataResults = getSearchResult(query, site);

        int countResult = dataResults.size();

        if (offset < dataResults.size()) {
            dataResults = dataResults.subList(offset, Math.min(offset + limit, dataResults.size()));
        } else {
            dataResults = Collections.emptyList();
        }

        return new SearchResponse(countResult, dataResults);
    }

    private boolean sitesNotIndexing(SiteEntity site) {
        if (site == null) {
            List<SiteEntity> siteList = siteRepository.findAll();
            for (SiteEntity s : siteList) {
                if (s.getStatus() == StatusEnum.INDEXED) {
                    return false;
                }
            }
        } else {
            return !(site.getStatus() == StatusEnum.INDEXED);
        }
        return true;
    }

    private List<SearchDataItem> getSearchResult(String query, SiteEntity site) {
        List<SearchDataItem> result = new ArrayList<>();

        if (site == null) {
            List<SiteEntity> siteList = siteRepository.findAll();
            for (SiteEntity s : siteList) {
                if (s.getStatus() == StatusEnum.INDEXED) {
                    result.addAll(getSearchDataItems(s, query));
                }
            }
        } else {
            result.addAll(getSearchDataItems(site, query));
        }

        result.sort(Comparator.comparing(SearchDataItem::getRelevance, Comparator.reverseOrder()).thenComparing(SearchDataItem::getUri));

        return result;
    }

    private List<SearchDataItem> getSearchDataItems(SiteEntity site, String query) {

        List<String> queryLemmas = new ArrayList<>(lemmatizer.lemmatize(query).keySet());

        List<SearchDataItem> result = new ArrayList<>();

        LinkedHashMap<String, Integer> sortedLemmas = getSortedLemmas(site, queryLemmas);

        if (sortedLemmas.isEmpty()) {
            return result;
        }

        Set<PageEntity> foundedPages = getFoundedPages(sortedLemmas, site);

        float rMax = 0;

        List<PageRankingResult> pageRankingResults = indexRepository.getTotalRankingForLemmasAndPages(queryLemmas, foundedPages);

        for (PageRankingResult pageRankingResult : pageRankingResults) {
            PageEntity page = pageRankingResult.getPage();
            float rAbs = pageRankingResult.getTotalRanking().floatValue();
            rMax = Math.max(rAbs, rMax);
            result.add(getSearchDataItem(page, queryLemmas, rAbs));
        }

        if (rMax > 0) {
            for (SearchDataItem dataItem : result) {
                dataItem.setRelevance(dataItem.getRelevance() / rMax);
            }
        }

        return result;
    }

    private SearchDataItem getSearchDataItem(PageEntity page, List<String> queryLemmas, float rAbs) {
        SearchDataItem dataItem = new SearchDataItem();
        dataItem.setSite(page.getSite().getUrl());
        dataItem.setUri(page.getPath());
        dataItem.setTitle(getPageTitle(page));
        dataItem.setSiteName(page.getSite().getName());
        dataItem.setSnippet(getSnippet(queryLemmas, page));
        dataItem.setRelevance(rAbs);
        return dataItem;
    }

    private LinkedHashMap<String, Integer> getSortedLemmas(SiteEntity site, List<String> queryLemmas) {
        SortedMap<String, Integer> lemmasFrequency = new TreeMap<>();

        for (String queryLemma : queryLemmas) {
            LemmaEntity lemma = lemmaRepository.findBySiteAndLemma(site, queryLemma);
            if (lemma == null) {
                continue;
            }

            lemmasFrequency.put(queryLemma, lemma.getFrequency());
        }

        return lemmasFrequency.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new)
                );
    }

    private Set<PageEntity> getFoundedPages(LinkedHashMap<String, Integer> sortedLemmas, SiteEntity site) {
        int limitPage = 3;
        String firstSortedLemma = sortedLemmas.entrySet().iterator().next().getKey();
        List<PageEntity> pages = pageRepository.findPagesBySiteIdAndLemma(site.getId(), firstSortedLemma);
        sortedLemmas.remove(firstSortedLemma);
        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {
            if (pages.size() > limitPage) {
                List<PageEntity> currentLemmaPages = pageRepository.findPagesBySiteIdAndLemma(site.getId(), entry.getKey());
                pages.retainAll(currentLemmaPages);
            }
        }
        return new HashSet<>(pages);
    }

    private String getPageTitle(PageEntity page) {
        Document html = Jsoup.parse(page.getContent());
        return html.title();
    }

    private String getSnippet(List<String> queryLemmas, PageEntity page) {
        String content = "";

        try {
            Document doc = Jsoup.parse(page.getContent());
            content = doc.body().text();
        } catch (Exception ignored) {
        }

        if (content.isBlank()) {
            return "";
        }

        String[] contentWords = content.trim().split("\\s+");
        List<String> practicedWords = new ArrayList<>();
        for (String contentWord : contentWords) {
            HashMap<String, Integer> contentWordLemmaMap = lemmatizer.lemmatize(contentWord);
            String workWord = contentWord.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
            for (String queryLemma : queryLemmas) {
                if (contentWordLemmaMap.get(queryLemma) != null && !practicedWords.contains(workWord)) {
                    content = content.replaceAll("(?<=\\s|^)" + workWord + "(?=\\s|$)", "<b>" + workWord + "</b>");
                    practicedWords.add(workWord);
                }
            }

        }

        return createSnippet(content);
    }

    private String createSnippet(String inputText) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("<b>[^<>]+</b>");
        Matcher matcher = pattern.matcher(inputText);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        if (count == 0) {
            return "";
        }
        int partLength = jsoupConfig.getSnippetLength() / count / 2;
        matcher.reset();
        int firstMatch = 0;
        int from = 0;
        int to = 0;
        int offset = -1;
        while (matcher.find()) {
            from = Math.max(from, matcher.start() - partLength);
            to = Math.min(inputText.length(), matcher.end() + partLength);

            if (offset == -1) {
                firstMatch = from;
            }

            if (from <= offset) {
                from = offset;
            }

            if (from != 0 && from != offset) {
                result.append("...");
            }

            result.append(inputText, from, to);

            offset = to;
        }

        if (result.toString().length() < jsoupConfig.getSnippetLength()) {
            int adding = (jsoupConfig.getSnippetLength() - result.toString().length()) / 2;
            int start = 0;
            if (result.toString().startsWith("...")) {
                start = 3;
            }
            result.insert(start, inputText.substring(Math.max(0, firstMatch - adding), firstMatch));
            result.append(inputText, to, Math.min(inputText.length(), to + adding));
        }

        if (to < inputText.length()) {
            result.append("...");
        }

        return result.toString();
    }
}