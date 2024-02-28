package gov.cabinetoffice.gap.service;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class SnsServiceTest {
    private final AmazonSNSClient snsClient = Mockito.mock(AmazonSNSClient.class);

    private final SnsService snsService = new SnsService(snsClient);

    private final String grantName = "Grant";

    private final long applicationCount = 100;

    @BeforeEach
    void beforeAll() throws Exception {
        reset(snsClient);
    }

    @Test
    void successfullyPublishesMessage() {
        final PublishResult mockResult = new PublishResult().withMessageId("mockMessageId");
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);

        final String result = snsService.failureInExport(grantName, applicationCount);

        assertThat(result).isEqualTo("Message with message ID: mockMessageId sent to SNS Topic: arn:partition:service:region:account-id:resource-id");
    }

    @Test
    void throwsException() {
        when(snsClient.publish(any())).thenThrow(new AmazonSNSException("error publishing message"));
        final String result = snsService.failureInExport(grantName, applicationCount);
        assertThat(result).isEqualTo("Error publishing message to SNS topic (arn:partition:service:region:account-id:resource-id) with error: error publishing message");
    }
}