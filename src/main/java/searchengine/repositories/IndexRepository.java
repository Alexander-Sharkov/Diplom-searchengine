package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.searching.PageRankingResult;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Set;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    IndexEntity findByPageAndLemma(PageEntity page, LemmaEntity lemma);

    @Modifying
    @Transactional
    @Query("UPDATE IndexEntity i SET i.ranking = :ranking WHERE i = :index")
    void updateIndexRanking(@Param("index") IndexEntity index, @Param("ranking") int ranking);

    @Query("SELECT new searchengine.dto.searching.PageRankingResult(i.page as page, COALESCE(SUM(i.ranking), 0.0) as ranking) FROM IndexEntity i WHERE i.lemma.lemma IN :lemmas AND i.page IN :pages GROUP BY i.page")
    List<PageRankingResult> getTotalRankingForLemmasAndPages(@Param("lemmas") List<String> lemmas, @Param("pages") Set<PageEntity> pages);
}
