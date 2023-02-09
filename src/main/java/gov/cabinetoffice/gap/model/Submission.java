package gov.cabinetoffice.gap.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    private UUID userId;

    public SubmissionSection getSectionById(String sectionId) {
        return this.sections.stream().filter(section -> Objects.equals(section.getSectionId(), sectionId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Section with id " + sectionId + " does not exist"));
    }

}
