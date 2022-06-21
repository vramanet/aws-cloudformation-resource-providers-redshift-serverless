package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.WorkgroupStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    protected static final Constant DELETE_BACKOFF_STRATEGY = Constant.of()
            .timeout(Duration.ofMinutes(5L))
            .delay(Duration.ofSeconds(5L))
            .build();

    protected static final Constant UPDATE_BACKOFF_STRATEGY = Constant.of()
            .timeout(Duration.ofMinutes(20L))
            .delay(Duration.ofSeconds(5L))
            .build();

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger);

    public boolean isWorkgroupStable(final Object awsRequest,
                                     final RedshiftServerlessResponse awsResponse,
                                     final ProxyClient<RedshiftServerlessClient> proxyClient,
                                     final ResourceModel model,
                                     final CallbackContext context) {
        GetWorkgroupRequest getStatusRequest = GetWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .build();

        try {
            String workgroupStatus = proxyClient.injectCredentialsAndInvokeV2(getStatusRequest, proxyClient.client()::getWorkgroup)
                    .workgroup()
                    .statusAsString();

            return workgroupStatus.equalsIgnoreCase(WorkgroupStatus.AVAILABLE.toString());

        } catch (ResourceNotFoundException e) {
            return awsRequest instanceof DeleteWorkgroupRequest;
        }
    }
}
