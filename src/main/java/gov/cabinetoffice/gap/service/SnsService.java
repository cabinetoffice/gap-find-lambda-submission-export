package gov.cabinetoffice.gap.service;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import gov.cabinetoffice.gap.lambda.Handler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Log4j2
public class SnsService {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    AmazonSNSClient snsClient;

    public SnsService (AmazonSNSClient snsClient) {
        this.snsClient = snsClient;
    }

    private String publishMessageToTopic(String subject, String body) {
        try {
            final PublishRequest request = new PublishRequest(System.getenv("TOPIC_ARN"), body, subject);
            final PublishResult result = snsClient.publish(request);
            return "Message with message ID:" + result.getMessageId() + " sent to SNS Topic: " + System.getenv("TOPIC_ARN");
        } catch (AmazonSNSException e) {
            return "Error publishing message to SNS topic (" + System.getenv("TOPIC_ARN") + ") with error: " + e.getErrorMessage();
        }

    }

    public String failureInExport(String grantName, long failureCount) {
        logger.info(String.format("An application download run for %s has encountered errors. This has caused %s applications to become unavailable to download. Sending email...", grantName, failureCount));

        //TODO: This copy is subject to change
        final String subject = String.format("%s - Application download run errors", grantName);
        final String body = String.format("An application download run for %s has encountered errors. This has caused %s applications to become unavailable to download.", grantName, failureCount);

        return publishMessageToTopic(subject, body);
    }

}