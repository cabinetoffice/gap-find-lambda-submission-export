package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.AddingSignedUrlDTO;
import gov.cabinetoffice.gap.model.OutstandingExportCountDTO;

public class ExportRecordService {

    public static void updateExportRecordStatus(String exportId, String submissionId, GrantExportStatus newStatus)
            throws Exception {

        final String postEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/status";

        RestService.sendPostRequest("\"" + newStatus.toString() + "\"", postEndpoint);
    }

    public static long getOutstandingExportsCount(String exportId) throws Exception {
        final String getEndpoint = "/export-batch/" + exportId + "/outstandingCount";

        return RestService.sendGetRequest(null, getEndpoint, OutstandingExportCountDTO.class).getOutstandingCount();
    }

    public static void addSignedUrlToExportRecord(String exportId, String submissionId, String signedUrl)
            throws Exception {
        final String patchEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/signedUrl";

        RestService.sendPatchRequest(new AddingSignedUrlDTO(signedUrl), patchEndpoint);
    }

}
