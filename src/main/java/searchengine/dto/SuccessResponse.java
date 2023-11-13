package searchengine.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuccessResponse extends BasicResponse {
    @Builder
    public SuccessResponse() {
        super(true);
    }
}
