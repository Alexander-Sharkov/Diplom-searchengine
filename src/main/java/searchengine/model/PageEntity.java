package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "pages", indexes = {
        @Index(name = "idx_site_path", columnList = "site_id, path")
})
@NoArgsConstructor
@Getter
@Setter
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;
    @Column(nullable = false, length = 512)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IndexEntity> indexes = new ArrayList<>();
    @Override
    public String toString() {
        return "PageEntity{" +
                "id=" + id +
                ", site=" + site.getUrl() +
                ", path='" + path + '\'' +
                ", code=" + code +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity page = (PageEntity) o;
        return Objects.equals(site, page.site) && Objects.equals(path, page.path);
    }
    @Override
    public int hashCode() {
        return Objects.hash(site, path);
    }
}
