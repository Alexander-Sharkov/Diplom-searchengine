package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;

@Service
@Slf4j
@AllArgsConstructor
public class PageService {

    private final PageRepository pageRepository;
    private final LemmaService lemmaService;

    @Transactional
    public void addPage(PageEntity page) {
        try {
            if (pageRepository.findBySiteAndPath(page.getSite(), page.getPath()) == null) {
                page = pageRepository.save(page);
                lemmaService.lemmatize(page, false);
                log.info("Добавлена страница - " + page.getSite().getUrl() + " - " + page.getPath());
            }
        } catch (Exception e) {
            log.error("in addPage - " + page.getSite().getUrl() + " - " + page.getPath() + " - " + e.getMessage(), e);
        }
    }
}
