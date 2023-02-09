package gov.cabinetoffice.gap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gov.cabinetoffice.gap.enums.ResponseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionQuestion {

    private UUID attachmentId;
    private String questionId;
    private String fieldTitle;
    private String hintText;
    private String displayText;
    private String questionSuffix;
    private String profileField;
    private String fieldPrefix;
    private String adminSummary;
    private ResponseTypeEnum responseType;
    private SubmissionQuestionValidation validation;
    private String[] options;
    private String response;
    private String[] multiResponse;

}
