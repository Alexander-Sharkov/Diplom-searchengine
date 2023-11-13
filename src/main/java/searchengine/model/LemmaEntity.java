package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "lemma", indexes = {
        @Index(name = "idx_site_lemma", columnList = "site_id, lemma")
})
@NoArgsConstructor
@Getter
@Setter
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;
    @Column(nullable = false)
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IndexEntity> indexes = new ArrayList<>();
    @Override
    public String toString() {
        return "LemmaEntity{" +
                "id=" + id +
                ", site=" + site.getUrl() +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntity lemma1 = (LemmaEntity) o;
        return Objects.equals(site, lemma1.site) && Objects.equals(lemma, lemma1.lemma);
    }
    @Override
    public int hashCode() {
        return Objects.hash(site, lemma);
    }
}
