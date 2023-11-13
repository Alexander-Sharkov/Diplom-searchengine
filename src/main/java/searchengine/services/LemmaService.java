package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.LemmaRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class LemmaService {
    private final LemmaRepository lemmaRepository;
    private final Lemmatizer lemmatizer;
    private final IndexService indexService;

    @Transactional
    public void lemmatize(PageEntity page, boolean reIndex) {
        String clearText = lemmatizer.clearText(page.getContent());
        HashMap<String, Integer> lemmas = lemmatizer.lemmatize(clearText);

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String l = entry.getKey();
            Integer f = entry.getValue();
            LemmaEntity lemma = lemmaRepository.findBySiteAndLemma(page.getSite(), l);
            if (lemma == null) {
                lemma = new LemmaEntity();
                lemma.setSite(page.getSite());
                lemma.setLemma(l);
                lemma.setFrequency(1);
                lemma = lemmaRepository.save(lemma);
            } else if (!reIndex) {
                lemmaRepository.updateLemmaFrequency(page.getSite(), l);
            }

            indexService.addIndex(page, lemma, f);
        }
    }
}
