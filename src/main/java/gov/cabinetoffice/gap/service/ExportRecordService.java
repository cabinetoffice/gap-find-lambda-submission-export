package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.AddingS3ObjectKeyDTO;
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

    public static void addS3ObjectKeyToExportRecord(OkHttpClient restClient, String exportId, String submissionId, String s3ObjectKey)
            throws Exception {
        final String patchEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/s3-object-key";

        RestService.sendPatchRequest(restClient, new AddingS3ObjectKeyDTO(s3ObjectKey), patchEndpoint);
    }

}
