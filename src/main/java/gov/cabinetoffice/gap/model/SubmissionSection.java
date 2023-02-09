package gov.cabinetoffice.gap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gov.cabinetoffice.gap.enums.SubmissionSectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionSection {
    private String sectionId;
    private String sectionTitle;
    private SubmissionSectionStatus sectionStatus;
    private List<SubmissionQuestion> questions;

    public SubmissionQuestion getQuestionById(String questionId) {
        return this.questions.stream().filter(question -> Objects.equals(question.getQuestionId(), questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Question with id " + questionId + " does not exist"));
    }
}
