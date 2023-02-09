package gov.cabinetoffice.gap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionQuestionValidation {

    private boolean mandatory;
    private Integer minLength;
    private Integer maxLength;
    private Integer minWords;
    private Integer maxWords;
    private boolean greaterThanZero;
    private String validInput;
    private int maxFileSizeMB;
    private String[] allowedTypes;

}
