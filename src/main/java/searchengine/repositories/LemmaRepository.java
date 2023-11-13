package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    LemmaEntity findBySiteAndLemma(SiteEntity site, String lemma);

    @Modifying
    @Transactional
    @Query("UPDATE LemmaEntity l SET l.frequency = l.frequency + 1 WHERE l.site = :site AND l.lemma = :lemma")
    void updateLemmaFrequency(@Param("site") SiteEntity site, @Param("lemma") String lemma);
}
