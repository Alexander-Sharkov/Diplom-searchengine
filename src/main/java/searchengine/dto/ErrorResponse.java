package searchengine.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse extends BasicResponse {
    private String error;

    @Builder
    public ErrorResponse(String error) {
        super(false);
        this.error = error;
    }
}
