package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.AddingSignedUrlDTO;
import gov.cabinetoffice.gap.model.OutstandingExportCountDTO;
import okhttp3.OkHttpClient;

public class ExportRecordService {

    public static void updateExportRecordStatus(OkHttpClient restClient, String exportId, String submissionId, GrantExportStatus newStatus)
            throws Exception {

        final String postEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/status";

        RestService.sendPostRequest(restClient, "\"" + newStatus.toString() + "\"", postEndpoint);
    }

    public static long getOutstandingExportsCount(OkHttpClient restClient, String exportId) throws Exception {
        final String getEndpoint = "/export-batch/" + exportId + "/outstandingCount";

        return RestService.sendGetRequest(restClient, null, getEndpoint, OutstandingExportCountDTO.class).getOutstandingCount();
    }

    public static void addSignedUrlToExportRecord(OkHttpClient restClient, String exportId, String submissionId, String signedUrl)
            throws Exception {
        final String patchEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/signedUrl";

        RestService.sendPatchRequest(restClient, new AddingSignedUrlDTO(signedUrl), patchEndpoint);
    }

}
