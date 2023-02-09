package gov.cabinetoffice.gap.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.amazonaws.services.kms.model.NotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CognitoServiceTest {

    private final static AWSCognitoIdentityProvider client = mock(AWSCognitoIdentityProvider.class);
    private final CognitoService cognitoService = new CognitoService();
    private static MockedStatic<AWSCognitoIdentityProviderClientBuilder> cognitoClientBuilder;

    @BeforeAll
    static void beforeAll() {
        cognitoClientBuilder = mockStatic(AWSCognitoIdentityProviderClientBuilder.class);
        cognitoClientBuilder.when(AWSCognitoIdentityProviderClientBuilder::defaultClient).thenReturn(client);
    }

    @AfterAll
    static void afterAll() {
        cognitoClientBuilder.close();
    }

    @Test
    void getUsersEmailAddressFromId_CallsAWS() {
        final ListUsersResult res = Mockito.mock(ListUsersResult.class);
        final UserType userType = new UserType().withAttributes(new AttributeType().withName("email").withValue("test@gmail.com"));
        final UUID userId = UUID.randomUUID();
        final ListUsersRequest listUsersRequest = new ListUsersRequest()
                .withFilter("sub = \"" + userId + "\"")
                .withUserPoolId("testUserPoolId");

        when(client.listUsers(any(ListUsersRequest.class))).thenReturn(res);
        when(res.getUsers()).thenReturn(Collections.singletonList(userType));

        cognitoService.getUsersEmailAddressFromId(userId);

        verify(client).listUsers(listUsersRequest);
    }

    @Test
    void getUsersEmailAddressFromId_ReturnsEmail() {
        final ListUsersResult res = Mockito.mock(ListUsersResult.class);
        final UserType userType = new UserType().withAttributes(new AttributeType().withName("email").withValue("test@gmail.com"));

        when(client.listUsers(any(ListUsersRequest.class))).thenReturn(res);
        when(res.getUsers()).thenReturn(Collections.singletonList(userType));

        final String email = cognitoService.getUsersEmailAddressFromId(UUID.randomUUID());

        assertEquals("test@gmail.com", email);
    }

    @Test
    void getUsersEmailAddressFromId_ThrowsUserNotFound() {
        final ListUsersResult res = Mockito.mock(ListUsersResult.class);

        when(client.listUsers(any(ListUsersRequest.class))).thenReturn(res);
        when(res.getUsers()).thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () -> cognitoService.getUsersEmailAddressFromId(UUID.randomUUID()));
    }

    @Test
    void getUsersEmailAddressFromId_ThrowsEmailNotFound() {
        final ListUsersResult res = Mockito.mock(ListUsersResult.class);
        final UserType userType = new UserType().withAttributes(new AttributeType().withValue("").withName(""));

        when(client.listUsers(any(ListUsersRequest.class))).thenReturn(res);
        when(res.getUsers()).thenReturn(Collections.singletonList(userType));

        assertThrows(NotFoundException.class, () -> cognitoService.getUsersEmailAddressFromId(UUID.randomUUID()));
    }
}
