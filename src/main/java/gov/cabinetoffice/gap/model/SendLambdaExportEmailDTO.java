package gov.cabinetoffice.gap.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SendLambdaExportEmailDTO {
    private String emailAddress;

    private UUID exportId;

    private UUID submissionId;

    private Map<String, String> personalisation;
}