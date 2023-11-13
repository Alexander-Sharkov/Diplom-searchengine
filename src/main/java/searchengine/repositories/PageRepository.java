package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    PageEntity findBySiteAndPath(@Param("site") SiteEntity site, @Param("path") String path);

    @Query("SELECT p FROM PageEntity p " +
            "JOIN IndexEntity i ON p.id = i.page.id " +
            "JOIN LemmaEntity l ON i.lemma.id = l.id " +
            "WHERE l.lemma = :lemma " +
            "AND p.site.id = :siteId")
    List<PageEntity> findPagesBySiteIdAndLemma(Integer siteId, String lemma);
}
