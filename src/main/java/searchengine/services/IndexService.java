package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;

@Service
@Slf4j
@AllArgsConstructor
public class IndexService {
    private final IndexRepository indexRepository;

    @Transactional
    public void addIndex(PageEntity page, LemmaEntity lemma, int ranking) {
        IndexEntity index = indexRepository.findByPageAndLemma(page, lemma);
        if (index == null) {
            index = new IndexEntity();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRanking(ranking);
            indexRepository.save(index);
        } else {
            if (index.getRanking() != ranking) {
                indexRepository.updateIndexRanking(index, ranking);
            }
        }
    }
}
