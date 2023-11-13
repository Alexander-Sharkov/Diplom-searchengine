package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Objects;

@Entity
@Table(name = "search_index", indexes = {
        @Index(name = "idx_page_lemma", columnList  = "page_id, lemma_id")
})
@NoArgsConstructor
@Getter
@Setter
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;
    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemma;
    @Column(nullable = false)
    private float ranking;
    @Override
    public String toString() {
        return "IndexEntity{" +
                "id=" + id +
                ", page=" + page.getPath() +
                ", lemma=" + lemma.getLemma() +
                ", ranking=" + ranking +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntity index = (IndexEntity) o;
        return Objects.equals(page, index.page) && Objects.equals(lemma, index.lemma);
    }
    @Override
    public int hashCode() {
        return Objects.hash(page, lemma);
    }
}
