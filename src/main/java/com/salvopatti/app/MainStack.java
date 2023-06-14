package com.salvopatti.app;

import com.hashicorp.cdktf.*;
import com.hashicorp.cdktf.providers.azuread.application.Application;
import com.hashicorp.cdktf.providers.azuread.provider.AzureadProvider;
import com.hashicorp.cdktf.providers.azuread.service_principal.ServicePrincipal;
import com.hashicorp.cdktf.providers.azuread.service_principal_password.ServicePrincipalPassword;
import com.hashicorp.cdktf.providers.azurerm.data_azurerm_storage_account_blob_container_sas.DataAzurermStorageAccountBlobContainerSas;
import com.hashicorp.cdktf.providers.azurerm.kubernetes_cluster.KubernetesCluster;
import com.hashicorp.cdktf.providers.azurerm.kubernetes_cluster.KubernetesClusterDefaultNodePool;
import com.hashicorp.cdktf.providers.azurerm.kubernetes_cluster.KubernetesClusterServicePrincipal;
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProvider;
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProviderFeatures;
import com.hashicorp.cdktf.providers.azurerm.resource_group.ResourceGroup;
import com.hashicorp.cdktf.providers.azurerm.storage_account.StorageAccount;
import com.hashicorp.cdktf.providers.azurerm.storage_container.StorageContainer;
import org.jetbrains.annotations.NotNull;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainStack extends TerraformStack {

    private static final String DNS_PREFIX = "testaksdnsprefix";
    private static final String NAME = "testaks";
    private static final String LOCATION = "westeurope";
    private final static String RESOURCE_GROUP_NAME = "kuberesourcegroup";

    //No warning that this should have been at least B2s
    private static final String VM_SIZE = "Standard_B2s";
    private static final String NODE_POOL_NAME = "kubenodepool";
    private static final Number OS_DISK_SIZE_GB = 30;

    //No warning that this should be an UUID
    private static final String APPLICATION_ID = "aaa5b2f6-9ce4-4b6f-88aa-09b58b6ddef1";
    private static final Map<String, String> TAGS = new HashMap<>();

    static {
        TAGS.put("env", "test");
        TAGS.put("project", "test-aks");
    }

    public MainStack(final Construct scope, final String id) {
        super(scope, id);

        AzurermProvider azureRMProvider = AzurermProvider.Builder
                .create(this, "azure")
                .features(AzurermProviderFeatures.builder()
                        .build())
                .build();

        AzureadProvider adProvider = AzureadProvider.Builder.create(this, "azuread")
                .build();

        /*new CloudBackend(this, CloudBackendConfig.builder()
                .hostname("s3.bucket.aws.com")
                .token("aaaaaaa")
                .organization("test")
                .workspaces(new NamedCloudWorkspace("dev-workspace"))
                .build());*/

        Application app = Application.Builder.create(this, "Kapp")
                .displayName("gdp-application").build();
        ServicePrincipal sp = ServicePrincipal.Builder.create(this, "KSP")
                .applicationId(app.getApplicationId()).build();
        ServicePrincipalPassword spPwd = ServicePrincipalPassword.Builder.create(this, "KSPPWD")
                .servicePrincipalId(sp.getObjectId()).build();

        KubernetesClusterServicePrincipal ksp = KubernetesClusterServicePrincipal.builder().
                clientId(sp.getApplicationId()).clientSecret(spPwd.getValue()).build();

        ResourceGroup resourceGroup = ResourceGroup.Builder.create(this, "aksresourcegroup")
                .name(RESOURCE_GROUP_NAME)
                .location(LOCATION)
                .build();

        //Builder pattern, errors about missing fields only after compilation
        KubernetesCluster aks = KubernetesCluster.Builder.create(this, "aks")
                .name(NAME)
                .dependsOn(Arrays.asList(resourceGroup))
                .location(LOCATION)
                .resourceGroupName(RESOURCE_GROUP_NAME)
                .dnsPrefix(DNS_PREFIX)
                .roleBasedAccessControlEnabled(true)
                .tags(TAGS)
                .servicePrincipal(ksp)
                .defaultNodePool(KubernetesClusterDefaultNodePool.builder()
                        .name(NODE_POOL_NAME)
                        .vmSize(VM_SIZE)
                        .nodeCount(1)
                        .enableAutoScaling(false)
                        .osDiskSizeGb(OS_DISK_SIZE_GB)
                        .build())
                .build();

        //FunctionApp f = FunctionApp.Builder.create(this, "function").build();
        //ServicebusQueue queue = ServicebusQueue.Builder.create(this, "queue").build();
    }
}