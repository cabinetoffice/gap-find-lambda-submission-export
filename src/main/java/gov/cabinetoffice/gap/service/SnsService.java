package gov.cabinetoffice.gap.service;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class SnsService {

    private static String publishMessageToTopic(String subject, String body) {
        try {
            AmazonSNSClient snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.defaultClient();

            //TODO: How do we store config for this lambda
            final PublishRequest request = new PublishRequest(System.getenv("PLACEHOLDER_TOPIC_ARN"), body, subject);
            final PublishResult result = snsClient.publish(request);

            log.info("Message published to SNS topic");

            return "Message with message id:" + result.getMessageId() + " sent.";
        } catch (AmazonSNSException e) {
            return "Error publishing message to SNS topic with error: " + e.getErrorMessage();
        }

    }

    public static String failureInExport(String grantName, long failureCount) {
        //TODO: This copy is subject to change
        final String subject = "Application download run failed";
        final String body = String.format("An application download run for %s has encountered errors. This has caused %s applications to become unavailable to download.", grantName, failureCount);

        return publishMessageToTopic(subject, body);
    }

}