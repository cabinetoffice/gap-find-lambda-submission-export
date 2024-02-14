package gov.cabinetoffice.gap.model;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class GrantExportDTO {
    private UUID exportBatchId;
    private UUID submissionId;
    private Integer applicationId;
    private GrantExportStatus status;
    private String emailAddress;
    private Instant created;
    private Integer createdBy;
    private Instant lastUpdated;
    private String location;
}
