package gov.cabinetoffice.gap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Submission {

    private String schemeId;
    private String schemeName;
    private String legalName;
    private String gapId;
    private Instant submittedDate;
    private List<SubmissionSection> sections;
    private String email;
    private Integer schemeVersion;

    public SubmissionSection getSectionById(String sectionId) {
        return this.sections.stream().filter(section -> Objects.equals(section.getSectionId(), sectionId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Section with id " + sectionId + " does not exist"));
    }

    public SubmissionQuestion getQuestionById(String sectionId, String questionId) {
        return this.getSectionById(sectionId)
                .getQuestions()
                .stream().filter(question -> question.getQuestionId().equals(questionId))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Question with id " + questionId + " does not exist"));
    }

}
