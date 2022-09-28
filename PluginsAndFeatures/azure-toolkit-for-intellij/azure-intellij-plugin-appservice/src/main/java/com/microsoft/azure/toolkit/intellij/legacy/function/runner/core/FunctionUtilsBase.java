/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.core;

import com.google.gson.JsonObject;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.azure.toolkit.ide.appservice.util.JsonUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.intellij.secure.IntelliJSecureStore;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FunctionUtilsBase {
    private static final int MAX_PORT = 65535;

    public static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";

    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[2.*, 3.0.0)\"}}\n";
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";
    private static final String AZURE_FUNCTIONS = "azure-functions";
    private static final String AZURE_FUNCTION_CUSTOM_BINDING_CLASS =
            "com.microsoft.azure.functions.annotation.CustomBinding";
    private static final Map<BindingEnum, List<String>> REQUIRED_ATTRIBUTE_MAP = new HashMap<>();
    private static final List<String> CUSTOM_BINDING_RESERVED_PROPERTIES = Arrays.asList("type", "name", "direction");
    private static final String AZURE_FUNCTIONS_APP_SETTINGS = "Azure Functions App Settings";
    private static final String AZURE_FUNCTIONS_JAVA_LIBRARY = "azure-functions-java-library";
    private static final String AZURE_FUNCTIONS_JAVA_CORE_LIBRARY = "azure-functions-java-core-library";

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.EventHubTrigger, Arrays.asList("cardinality"));
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.HttpTrigger, Arrays.asList("authLevel"));
    }

    public static void saveAppSettingsToSecurityStorage(String key, Map<String, String> appSettings) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        final String appSettingsJsonValue = JsonUtils.toJsonString(appSettings);
        IntelliJSecureStore.getInstance().savePassword(AZURE_FUNCTIONS_APP_SETTINGS, key, null, appSettingsJsonValue);
    }

    public static Map<String, String> loadAppSettingsFromSecurityStorage(String key) {
        if (StringUtils.isEmpty(key)) {
            return new HashMap<>();
        }
        final String value = IntelliJSecureStore.getInstance().loadPassword(AZURE_FUNCTIONS_APP_SETTINGS, key, null);
        return StringUtils.isEmpty(value) ? new HashMap<>() : JsonUtils.fromJson(value, Map.class);
    }

    public static File getTempStagingFolder() {
        try {
            final Path path = Files.createTempDirectory(AZURE_FUNCTIONS);
            final File file = path.toFile();
            FileUtils.forceDeleteOnExit(file);
            return file;
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("failed to get temp staging folder", e);
        }
    }

    @AzureOperation(
        name = "function.clean_staging_folder.folder",
        params = {"stagingFolder.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static void cleanUpStagingFolder(File stagingFolder) {
        try {
            if (stagingFolder != null) {
                FileUtils.deleteDirectory(stagingFolder);
            }
        } catch (final IOException e) {
            // swallow exceptions while clean up
        }
    }

    @AzureOperation(
        name = "common.validate_func_project.project",
        params = {"project.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static boolean isFunctionProject(Project project) {
        if (project == null) {
            return false;
        }
        final List<Library> libraries = new ArrayList<>();
        OrderEnumerator.orderEntries(project).productionOnly().forEachLibrary(library -> {
            if (StringUtils.contains(library.getName(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                libraries.add(library);
            }
            return true;
        });
        return libraries.size() > 0;
    }

    public static Path getDefaultHostJson(Project project) {
        return new File(project.getBasePath(), "host.json").toPath();
    }

    public static @Nullable Path createTempleHostJson() {
        try {
            final File result = File.createTempFile("host", ".json");
            FileUtils.write(result, DEFAULT_HOST_JSON, Charset.defaultCharset());
            return result.toPath();
        } catch (final IOException e) {
            return null;
        }
    }

    @AzureOperation(
        name = "function.copy_settings.settings|folder",
        params = {"localSettingJson", "stagingFolder"},
        type = AzureOperation.Type.TASK
    )
    public static void copyLocalSettingsToStagingFolder(Path stagingFolder,
                                                        Path localSettingJson,
                                                        Map<String, String> appSettings) throws IOException {
        final File localSettingsFile = new File(stagingFolder.toFile(), "local.settings.json");
        copyFilesWithDefaultContent(localSettingJson, localSettingsFile, DEFAULT_LOCAL_SETTINGS_JSON);
        if (MapUtils.isNotEmpty(appSettings)) {
            updateLocalSettingValues(localSettingsFile, appSettings);
        }
    }

    public static String getFuncPath() throws IOException, InterruptedException {
        final AzureConfiguration config = Azure.az().config();
        if (StringUtils.isBlank(config.getFunctionCoreToolsPath())) {
            return FunctionCliResolver.resolveFunc();
        }
        return config.getFunctionCoreToolsPath();
    }

    public static List<String> getFunctionBindingList(Map<String, FunctionConfiguration> configMap) {
        return configMap.values().stream().flatMap(configuration -> configuration.getBindings().stream())
                        .map(Binding::getType)
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
    }

    protected static void writeFunctionJsonFile(File file, FunctionConfiguration config) throws IOException {
        final Map<String, Object> json = new LinkedHashMap<>();
        json.put("scriptFile", config.getScriptFile());
        json.put("entryPoint", config.getEntryPoint());
        final List<Map<String, Object>> lists = new ArrayList<>();
        if (config.getBindings() != null) {
            for (final Binding binding : config.getBindings()) {
                final Map<String, Object> bindingJson = new LinkedHashMap<>();
                bindingJson.put("type", binding.getType());
                bindingJson.put("direction", binding.getDirection());
                bindingJson.put("name", binding.getName());
                final Map<String, Object> attributes = binding.getBindingAttributes();
                for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
                    // Skip 'name' property since we have serialized before the for-loop
                    if (bindingJson.containsKey(entry.getKey())) {
                        continue;
                    }
                    bindingJson.put(entry.getKey(), entry.getValue());
                }
                lists.add(bindingJson);
            }
            json.put("bindings", lists.toArray());
        }
        file.getParentFile().mkdirs();
        JsonUtils.writeJsonToFile(file, json);
    }

    protected static String stripExtraCharacters(String fileName) {
        // TODO-dp this is not robust enough (eliminated !/ at the end of the jar)
        return StringUtils.endsWith(fileName, "!/") ?
               fileName.substring(0, fileName.length() - 2) : fileName;
    }

    private static Binding getHTTPOutBinding() {
        final Binding result = new Binding(BindingEnum.HttpOutput);
        result.setName(HTTP_OUTPUT_DEFAULT_NAME);
        return result;
    }

    protected static void copyFilesWithDefaultContent(Path sourcePath, File dest, String defaultContent)
            throws IOException {
        final File src = sourcePath == null ? null : sourcePath.toFile();
        if (src != null && src.exists()) {
            FileUtils.copyFile(src, dest);
        } else {
            FileUtils.write(dest, defaultContent, Charset.defaultCharset());
        }
    }

    private static void updateLocalSettingValues(File target, Map<String, String> appSettings) throws IOException {
        final JsonObject jsonObject = ObjectUtils.firstNonNull(JsonUtils.readJsonFile(target), new JsonObject());
        final JsonObject valueObject = new JsonObject();
        appSettings.entrySet().forEach(entry -> valueObject.addProperty(entry.getKey(), entry.getValue()));
        jsonObject.add("Values", valueObject);
        JsonUtils.writeJsonToFile(target, jsonObject);
    }

    protected static boolean isModuleInTestScope(Module module) {
        if (module == null) {
            return false;
        }
        final CompilerModuleExtension cme = CompilerModuleExtension.getInstance(module);
        if (cme == null) {
            return false;
        }
        return cme.getCompilerOutputUrl() == null && cme.getCompilerOutputUrlForTests() != null;
    }

    public static int findFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }

    public static int findFreePort(int startPort, int... skipPorts) {
        ServerSocket socket = null;
        for (int port = startPort; port <= MAX_PORT; port++) {
            if (!ArrayUtils.contains(skipPorts, port) && isPortFree(port)) {
                return port;
            }
        }
        return -1;
    }

    private static final boolean isPortFree(int port) {
        return SystemInfo.isMac ? isPortFreeInMac(port) : isPortFreeInWindows(port);
    }

    private static final boolean isPortFreeInWindows(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static final boolean isPortFreeInMac(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // setReuseAddress(false) is required only on OSX,
            // otherwise the code will not work correctly on that platform
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
