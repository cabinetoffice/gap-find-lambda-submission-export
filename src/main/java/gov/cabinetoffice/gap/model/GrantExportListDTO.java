package gov.cabinetoffice.gap.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class GrantExportListDTO {
    private UUID exportBatchId;
    private List<GrantExportDTO> grantExports;
}
