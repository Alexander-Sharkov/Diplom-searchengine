package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.BasicResponse;
import searchengine.dto.ErrorResponse;
import searchengine.dto.SuccessResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusEnum;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class IndexingService {
    public static AtomicBoolean isRunning = new AtomicBoolean(false);

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final JsoupConfig jsoupConfig;
    private final PageService pageService;
    private final LemmaService lemmaService;

    private final Set<String> excludedExtensions = Collections.synchronizedSet(new HashSet<>(
            Arrays.asList(
                    "pdf", "txt", "djv", "djvu", "chm",
                    "doc", "docx", "csv", "xls", "xlsx",
                    "zip", "nc", "jpg", "ppt", "fig",
                    "m", "png", "tiff", "bmp", "jpeg",
                    "rar", "7z", "xml", "mp4", "gif",
                    "ics", "sig", "pptx", "rtf", "dot"
            )
    ));

    public BasicResponse startIndexing() {
        if (isRunning.get()) {
            return new ErrorResponse("Индексация уже запущена");
        }

        isRunning.set(true);

        indexing();

        return new SuccessResponse();
    }

    public void indexing() {
        List<SiteConfig> siteList = sites.getSites();
        siteList.forEach(s -> {
            SiteEntity site = siteRepository.findByUrl(s.getUrl());
            if (site != null) {
                deleteDataForSite(site);
            }
            site = createSite(s);

            updateSiteStatus(site, StatusEnum.INDEXING);
            log.info("Сайт - " + site.getUrl() + " - Запущена индексация");

            SiteEntity finalSite = site;
            CompletableFuture
                    .runAsync(() -> scan(finalSite, finalSite.getUrl()), ForkJoinPool.commonPool())
                    .thenAccept(x -> {
                        if (isRunning.get()) {
                            updateSiteStatus(finalSite, StatusEnum.INDEXED);
                            log.info("Сайт - " + finalSite.getUrl() + " проиндексирован");
                            List<SiteEntity> sites = siteRepository.findAll();
                            sites.forEach(checkSite -> {
                                if (checkSite.getStatus().equals(StatusEnum.INDEXING)) {
                                    isRunning.set(true);
                                }
                            });
                            if (sites.stream().allMatch(ss -> ss.getStatus() != StatusEnum.INDEXING)) {
                                isRunning.set(false);
                            }
                        } else {
                            updateSiteStatus(finalSite, StatusEnum.FAILED, "Индексация остановлена пользователем");
                            log.info("Сайт - " + finalSite.getUrl() + " остановлена индексация");
                        }
                    })
                    .exceptionally(e -> {
                        log.error(e.getMessage());
                        updateSiteStatus(finalSite, StatusEnum.FAILED, e.getMessage());
                        siteRepository.save(finalSite);
                        return null;
                    });
        });
    }

    private void scan(SiteEntity site, String url) {
        try {
            Document doc = getDoc(url);
            Thread.sleep(200);

            PageEntity page = createPage(site, url, doc);

            pageService.addPage(page);

            Elements elements = doc.select("a");
            Set<String> links = elements.stream().map(e -> e.absUrl("href")).map(String::trim).collect(Collectors.toSet());
            for (String link : links) {
                if (!isRunning.get()) {
                    return;
                }
                link = link.endsWith("/") ? link.substring(0, link.length() - 1) : link;

                if (!link.contains("://www.") && site.getUrl().contains("://www.")) {
                    link = link.replaceFirst("://", "://www.");
                }

                link = link.replace(" ", "%20");
                link = link.replace("[", "%5B");
                link = link.replace("]", "%5D");
                link = link.replace("{", "%7B");
                link = link.replace("}", "%7D");
                if (isCorrectUrl(site, link)) {
                    scan(site, link);
                }
            }
        } catch (HttpStatusException e) {
            PageEntity page = new PageEntity();
            page.setSite(site);
            page.setPath(getPathFromUrl(url));
            page.setCode(e.getStatusCode());
            page.setContent("");
            pageService.addPage(page);
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            log.error(url + " - " + e.getMessage(), e);
        }
    }

    private Document getDoc(String url) throws IOException {
        return Jsoup.connect(url).userAgent(jsoupConfig.getUserAgent()).referrer(jsoupConfig.getReferrer()).get();
    }

    private PageEntity createPage(SiteEntity site, String url, Document doc) {
        PageEntity page = new PageEntity();
        page.setSite(site);
        page.setPath(getPathFromUrl(url));
        page.setCode(doc.connection().response().statusCode());
        page.setContent(doc.outerHtml());
        return page;
    }

    private String getPathFromUrl(String urlString) {
        try {
            URL url = new URI(urlString).toURL();
            return url.getPath().isEmpty() ? "/" : url.getPath();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "";
    }

    private boolean isCorrectUrl(SiteEntity site, String url) {
        if (!url.contains(site.getUrl())) {
            return false;
        }
        if (!url.startsWith("http") || url.contains("#") || url.contains("?")) {
            return false;
        }

        for (String e : excludedExtensions) {
            if (url.toLowerCase().endsWith(e)) {
                return false;
            }
        }

        PageEntity page = pageRepository.findBySiteAndPath(site, getPathFromUrl(url));
        return page == null;
    }

    public BasicResponse stopIndexSite() {
        if (!isRunning.get()) {
            return new ErrorResponse("Индексация не запущена");
        }

        isRunning.set(false);

        return new SuccessResponse();
    }

    private SiteEntity createSite(SiteConfig siteConfig) {
        SiteEntity site = siteRepository.findByUrl(siteConfig.getUrl());

        if (site == null) {
            site = new SiteEntity();
            site.setUrl(siteConfig.getUrl());
            site.setName(siteConfig.getName());
        }

        site.setStatusTime(LocalDateTime.now());
        return siteRepository.save(site);
    }

    private void deleteDataForSite(SiteEntity site) {
        log.info("Delete - " + site.getName());
        siteRepository.deleteById(site.getId());
    }

    private void updateSiteStatus(SiteEntity site, StatusEnum status) {
        updateSiteStatus(site, status, "");
    }

    private void updateSiteStatus(SiteEntity site, StatusEnum status, String error) {
        site.setLastError(error);
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        System.out.println("Статус: " + status + " для " + site.getName());
    }

    public BasicResponse indexPage(String url) {
        List<SiteConfig> siteConfigList = sites.getSites();
        List<SiteEntity> siteList = siteRepository.findAll();

        boolean urlInConfigList = siteConfigList.stream().anyMatch(s -> url.contains(s.getUrl()));
        boolean urlInRepository = siteList.stream().anyMatch(s -> url.contains(s.getUrl()));

        if (!urlInConfigList && !urlInRepository) {
            return new ErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        new Thread(() -> {
            SiteEntity site = urlInRepository ?
                    siteList.stream().filter(s -> url.contains(s.getUrl())).findFirst().get() :
                    createSite(siteConfigList.stream().filter(s -> url.contains(s.getUrl())).findFirst().get());

            try {
                Document doc = getDoc(url);

                boolean reIndex = false;

                PageEntity page = pageRepository.findBySiteAndPath(site, getPathFromUrl(url));

                if (page == null) {
                    page = new PageEntity();
                    page.setSite(site);
                    page.setPath(getPathFromUrl(url));
                } else {
                    reIndex = true;
                }

                page.setCode(doc.connection().response().statusCode());
                page.setContent(doc.outerHtml());
                page = pageRepository.save(page);

                lemmaService.lemmatize(page, reIndex);

                log.info("Проиндексирована страница - " + url);
            } catch (HttpStatusException e) {
                log.error("Ошибка - " + e.getStatusCode(), e);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }).start();

        return new SuccessResponse();
    }
}