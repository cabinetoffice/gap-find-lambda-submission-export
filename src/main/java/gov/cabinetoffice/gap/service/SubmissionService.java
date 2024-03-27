package gov.cabinetoffice.gap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.model.SubmissionSection;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    public static void addSubmissionSectionsJsonToSubmissionModel(Submission submissionModel,
            String submissionSectionsJson) throws JsonMappingException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        List<SubmissionSection> sections = om.readValue(submissionSectionsJson,
                new TypeReference<List<SubmissionSection>>() {
                });

        submissionModel.setSections(sections);

    }

    public static Submission getSubmissionData(OkHttpClient restClient, String batchId, String submissionId) throws Exception {
        String getEndpoint = "/submissions/" + submissionId + "/export-batch/" + batchId + "/submission";
        logger.info("Sending getRequest to {}", getEndpoint);
        return RestService.sendGetRequest(restClient, null, getEndpoint, Submission.class);
    }
}
