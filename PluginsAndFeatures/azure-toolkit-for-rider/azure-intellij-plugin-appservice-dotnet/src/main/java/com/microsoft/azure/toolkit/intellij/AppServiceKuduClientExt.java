/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij;

import com.azure.core.annotation.*;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.RestProxy;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Objects;

// TODO: Move it to the base plugin (https://github.com/microsoft/azure-maven-plugins/pull/2511)
public class AppServiceKuduClientExt
{
    private final String host;
    private final KuduServiceExt kuduService;

    private AppServiceKuduClientExt(String host, KuduServiceExt kuduService) {
        this.host = host;
        this.kuduService = kuduService;
    }

    public static AppServiceKuduClientExt getClient(@Nonnull WebAppBase webAppBase) {
        // refers : https://github.com/Azure/azure-sdk-for-java/blob/master/sdk/resourcemanager/azure-resourcemanager-appservice/src/main/java/
        // com/azure/resourcemanager/appservice/implementation/KuduClient.java
        if (webAppBase.defaultHostname() == null) {
            throw new AzureToolkitRuntimeException("Cannot initialize kudu client before web app is created");
        }
        String host = webAppBase.defaultHostname().toLowerCase(Locale.ROOT)
                .replace("http://", "")
                .replace("https://", "");
        String[] parts = host.split("\\.", 2);
        host = parts[0] + ".scm." + parts[1];
        host = "https://" + host;

        final KuduServiceExt kuduService = RestProxy.create(KuduServiceExt.class, webAppBase.manager().httpPipeline());
        return new AppServiceKuduClientExt(host, kuduService);
    }

    public ExtensionInfo getPackageFromRemoteStore(final @Nonnull String id) {
        var response = Objects.requireNonNull(this.kuduService.getPackageFromRemoteStore(host, id).block());
        if (response.getStatusCode() == 404) return null;

        return response.getValue();
    }

    public ExtensionInfo getInstalledPackage(final @Nonnull String id) {
        var response = Objects.requireNonNull(this.kuduService.getInstalledPackage(host, id).block());
        if (response.getStatusCode() == 404) return null;

        return response.getValue();
    }

    public ExtensionInfo installOrUpdatePackage(final @Nonnull String id) {
        return Objects.requireNonNull(this.kuduService.installOrUpdatePackage(host, id).block()).getValue();
    }

    public void killKuduProcess() {
        killProcess(0);
    }

    public void killProcess(final int id) {
        this.kuduService.killProcess(host, id).block();
    }

    @Host("{$host}")
    @ServiceInterface(name = "KuduServiceExt")
    private interface KuduServiceExt {
        @Headers("Content-Type: application/json; charset=utf-8")
        @Get("/api/extensionfeed/{id}")
        @ExpectedResponses({200, 404})
        Mono<Response<ExtensionInfo>> getPackageFromRemoteStore(@HostParam("$host") String host, @PathParam("id") String id);

        @Headers("Content-Type: application/json; charset=utf-8")
        @Get("/api/siteextensions/{id}")
        @ExpectedResponses({200, 404})
        Mono<Response<ExtensionInfo>> getInstalledPackage(@HostParam("$host") String host, @PathParam("id") String id);

        @Headers("Content-Type: application/json; charset=utf-8")
        @Put("/api/siteextensions/{id}")
        Mono<Response<ExtensionInfo>> installOrUpdatePackage(@HostParam("$host") String host, @PathParam("id") String id);

        @Delete("/api/processes/{id}")
        @ExpectedResponses({502})
        Mono<Void> killProcess(@HostParam("$host") String host, @PathParam("id") int id);
    }
}
