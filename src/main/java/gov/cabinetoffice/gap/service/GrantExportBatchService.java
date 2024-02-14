package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.AddingS3ObjectKeyDTO;
import okhttp3.OkHttpClient;

public class GrantExportBatchService {

    public static void updateGrantExportBatchRecordStatus(OkHttpClient restClient, String exportId, GrantExportStatus newStatus)
            throws Exception {

        final String patchEndpoint = "/grant-export-batch/" + exportId + "/status";
        RestService.sendPatchRequest(restClient, "\"" + newStatus.toString() + "\"", patchEndpoint);
    }

    public static void addS3ObjectKeyToGrantExportBatchRecord(OkHttpClient restClient, String exportId, String s3ObjectKey)
            throws Exception {

        final String patchEndpoint = "/grant-export-batch/" + exportId + "/s3-object-key";
        RestService.sendPatchRequest(restClient, new AddingS3ObjectKeyDTO(s3ObjectKey), patchEndpoint);
    }

}
