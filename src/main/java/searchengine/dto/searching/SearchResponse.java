package searchengine.dto.searching;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import searchengine.dto.BasicResponse;

import java.util.List;

@Getter
@Setter
public class SearchResponse extends BasicResponse {

    private int count;
    private List<SearchDataItem> data;

    @Builder
    public SearchResponse(int count, List<SearchDataItem> data) {
        super(true);
        this.count = count;
        this.data = data;
    }
}
