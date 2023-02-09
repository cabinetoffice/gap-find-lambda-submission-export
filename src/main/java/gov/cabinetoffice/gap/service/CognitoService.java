package gov.cabinetoffice.gap.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.kms.model.NotFoundException;

import java.util.UUID;

public class CognitoService {

    private final String USER_POOL_ID = System.getenv("USER_POOL_ID");
    private final AWSCognitoIdentityProvider cognitoClient = AWSCognitoIdentityProviderClientBuilder.defaultClient();

    public String getUsersEmailAddressFromId(final UUID id) {
        final ListUsersRequest listUsersRequest = new ListUsersRequest()
                .withFilter("sub = \"" + id + "\"")
                .withUserPoolId(USER_POOL_ID);

        return cognitoClient.listUsers(listUsersRequest).getUsers()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Could not find a user with the sub " + id))
                .getAttributes()
                .stream()
                .filter(attributeType -> attributeType.getName().equals("email"))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Could not find the email attribute on this user"))
                .getValue();
    }
}
