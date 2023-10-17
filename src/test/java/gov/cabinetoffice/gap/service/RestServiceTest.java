package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.model.OutstandingExportCountDTO;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RestServiceTest {

    private static final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);

    ArgumentCaptor<Request> httpRequestCaptor = ArgumentCaptor.forClass(Request.class);

    @BeforeEach
    void beforeEach() {
        reset(mockedHttpClient);
    }

    @Nested
    class sendGetRequest {

        @Test
        void shouldSuccessfullyCompleteGetRequest_WithoutParams() throws Exception {

            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            final ResponseBody mockResponseBody = mock(ResponseBody.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);
            when(mockResponse.body()).thenReturn(mockResponseBody);
            when(mockResponseBody.string()).thenReturn("{ \"outstandingCount\": 1 }");

            final OutstandingExportCountDTO expectedResponse = new OutstandingExportCountDTO(1l);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendGetRequest(any(), any(), anyString(), any()))
                        .thenCallRealMethod();

                OutstandingExportCountDTO response = RestService.sendGetRequest(mockedHttpClient,null, "/test/url",
                        OutstandingExportCountDTO.class);

                verify(mockedHttpClient).newCall(httpRequestCaptor.capture());
                Request capturedRequest = httpRequestCaptor.getValue();

                assertEquals("GET", capturedRequest.method());
                assertEquals("http://localhost:8080/api/test/url", capturedRequest.url().toString());
                assertEquals(expectedResponse, response);

            }

        }

        @Test
        void shouldSuccessfullyCompleteGetRequest_WithParams() throws Exception {

            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            final ResponseBody mockResponseBody = mock(ResponseBody.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);
            when(mockResponse.body()).thenReturn(mockResponseBody);
            when(mockResponseBody.string()).thenReturn("{ \"outstandingCount\": 1 }");

            final OutstandingExportCountDTO expectedResponse = new OutstandingExportCountDTO(1l);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendGetRequest(any(), anyMap(), anyString(), any()))
                        .thenCallRealMethod();

                Map<String, String> params = Map.of("newParam", "paramValue");

                OutstandingExportCountDTO response = RestService.sendGetRequest(mockedHttpClient, params, "/test/url",
                        OutstandingExportCountDTO.class);

                verify(mockedHttpClient).newCall(httpRequestCaptor.capture());
                Request capturedRequest = httpRequestCaptor.getValue();

                assertEquals("GET", capturedRequest.method());
                assertEquals("http://localhost:8080/api/test/url?newParam=paramValue",
                        capturedRequest.url().toString());
                assertEquals(expectedResponse, response);

            }

        }

        @Test
        void shouldThrowRuntimeExceptionWhenRequestIsUnsuccessful() throws Exception {

            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            final ResponseBody mockResponseBody = mock(ResponseBody.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(false);

            final OutstandingExportCountDTO expectedResponse = new OutstandingExportCountDTO(1l);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendGetRequest(any(), any(), anyString(), any()))
                        .thenCallRealMethod();

                assertThrows(RuntimeException.class,
                        () -> RestService.sendGetRequest(mockedHttpClient, null, "/test/url", OutstandingExportCountDTO.class));
            }
        }

    }

    @Nested
    class sendPostRequest {

        @Test
        void shouldSuccessfullyCompletePostRequestWithDTOBody() throws Exception {
            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                OutstandingExportCountDTO bodyDTO = new OutstandingExportCountDTO(1l);

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.executePost(any(), any(), anyString())).thenCallRealMethod();
                mockedRestService
                        .when(() -> RestService.sendPostRequest(any(), any(OutstandingExportCountDTO.class), anyString()))
                        .thenCallRealMethod();

                RestService.sendPostRequest(mockedHttpClient, bodyDTO, "/test/url");

                verify(mockedHttpClient).newCall(httpRequestCaptor.capture());
                Request capturedRequest = httpRequestCaptor.getValue();

                final Buffer bufferToReadBody = new Buffer();
                capturedRequest.body().writeTo(bufferToReadBody);

                assertEquals("POST", capturedRequest.method());
                assertEquals("http://localhost:8080/api/test/url", capturedRequest.url().toString());
                assertEquals("{\"outstandingCount\":1}", bufferToReadBody.readUtf8());

            }
        }

        @Test
        void shouldSuccessfullyCompletePostRequestWithStringBody() throws Exception {
            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.executePost(any(), any(), anyString())).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendPostRequest(any(), any(), anyString())).thenCallRealMethod();

                RestService.sendPostRequest(mockedHttpClient, "MockBodyValue", "/test/url");

                verify(mockedHttpClient).newCall(httpRequestCaptor.capture());
                Request capturedRequest = httpRequestCaptor.getValue();

                final Buffer bufferToReadBody = new Buffer();
                capturedRequest.body().writeTo(bufferToReadBody);

                assertEquals("POST", capturedRequest.method());
                assertEquals("http://localhost:8080/api/test/url", capturedRequest.url().toString());
                assertEquals("MockBodyValue", bufferToReadBody.readUtf8());

            }
        }

        @Test
        void shouldThrowRuntimeExceptionWhenRequestIsUnsuccessful() throws Exception {
            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(false);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.executePost(any(), any(), anyString())).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendPostRequest(any(), any(), anyString())).thenCallRealMethod();

                assertThrows(RuntimeException.class, () -> RestService.sendPostRequest(mockedHttpClient, "MockBodyValue", "/test/url"));

            }
        }

    }

    @Nested
    class sendPatchRequest {

        @Test
        void shouldSuccessfullyCompletePatchRequest() throws Exception {

            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                OutstandingExportCountDTO bodyDTO = new OutstandingExportCountDTO(1l);

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendPatchRequest(any(), any(), anyString())).thenCallRealMethod();

                RestService.sendPatchRequest(mockedHttpClient, bodyDTO, "/test/url");

                verify(mockedHttpClient).newCall(httpRequestCaptor.capture());
                Request capturedRequest = httpRequestCaptor.getValue();

                final Buffer bufferToReadBody = new Buffer();
                capturedRequest.body().writeTo(bufferToReadBody);

                assertEquals("PATCH", capturedRequest.method());
                assertEquals("http://localhost:8080/api/test/url", capturedRequest.url().toString());
                assertEquals("{\"outstandingCount\":1}", bufferToReadBody.readUtf8());

            }
        }

        void shouldThrowRuntimeExceptionWhenRequestIsUnsuccessful() throws Exception {
            final Call mockCall = mock(Call.class);
            final Response mockResponse = mock(Response.class);
            when(mockedHttpClient.newCall(any())).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(false);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {

                OutstandingExportCountDTO bodyDTO = new OutstandingExportCountDTO(1l);

                mockedRestService.when(() -> RestService.defaultRequestBuilder()).thenCallRealMethod();
                mockedRestService.when(() -> RestService.sendPatchRequest(any(), any(), anyString())).thenCallRealMethod();

                assertThrows(RuntimeException.class, () -> RestService.sendPatchRequest(mockedHttpClient, bodyDTO, "/test/url"));
            }
        }

    }

}
