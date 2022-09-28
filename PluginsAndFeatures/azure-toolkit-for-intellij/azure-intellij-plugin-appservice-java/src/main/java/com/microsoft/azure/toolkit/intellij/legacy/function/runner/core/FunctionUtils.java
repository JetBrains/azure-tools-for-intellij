/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.core;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.lang.jvm.JvmAnnotation;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureBundle;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.STORAGE_ACCOUNT;

public class FunctionUtils extends FunctionUtilsBase {
    private static final String AZURE_FUNCTION_ANNOTATION_CLASS =
            "com.microsoft.azure.functions.annotation.FunctionName";
    private static final String FUNCTION_JSON = "function.json";

    @AzureOperation(
            name = "function.list_function_modules.project",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static Module[] listFunctionModules(Project project) {
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        return Arrays.stream(modules).filter(m -> {
            if (isModuleInTestScope(m)) {
                return false;
            }
            final GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(m);
            final Callable<PsiClass> psiClassSupplier = () -> JavaPsiFacade.getInstance(project).findClass(AZURE_FUNCTION_ANNOTATION_CLASS, scope);
            final PsiClass ecClass = AzureTaskManager.getInstance().readAsObservable(new AzureTask<>(psiClassSupplier)).toBlocking().first();
            return ecClass != null;
        }).toArray(Module[]::new);
    }

    public static Module getFunctionModuleByName(Project project, String name) {
        final Module[] modules = listFunctionModules(project);
        return Arrays.stream(modules)
                .filter(module -> StringUtils.equals(name, module.getName()))
                .findFirst().orElse(null);
    }

    @AzureOperation(
            name = "function.list_function_methods.module",
            params = {"module.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static PsiMethod[] findFunctionsByAnnotation(Module module) {
        final PsiClass functionNameClass = JavaPsiFacade.getInstance(module.getProject())
                .findClass(AZURE_FUNCTION_ANNOTATION_CLASS,
                        GlobalSearchScope.moduleWithLibrariesScope(module));
        final List<PsiMethod> methods = new ArrayList<>(AnnotatedElementsSearch
                .searchPsiMethods(functionNameClass,
                        GlobalSearchScope.moduleScope(module))
                .findAll());
        return methods.toArray(new PsiMethod[0]);
    }

    public static boolean isFunctionClassAnnotated(final PsiMethod method) {
        try {
            return MetaAnnotationUtil.isMetaAnnotated(method,
                    ContainerUtil.immutableList(FunctionUtils.AZURE_FUNCTION_ANNOTATION_CLASS));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @AzureOperation(
            name = "function.prepare_staging_folder",
            type = AzureOperation.Type.TASK
    )
    public static Map<String, FunctionConfiguration> prepareStagingFolder(Path stagingFolder, Path hostJson, Project project, Module module, PsiMethod[] methods)
            throws AzureExecutionException, IOException {
        final Map<String, FunctionConfiguration> configMap = ReadAction.compute(() -> generateConfigurations(methods));
        if (stagingFolder.toFile().isDirectory()) {
            FileUtils.cleanDirectory(stagingFolder.toFile());
        }

        final Path jarFile;
        // test if it is gradle project
        final IntellijGradleFunctionProject gradleProject = new IntellijGradleFunctionProject(project, module);
        if (gradleProject.isValid() && gradleProject.getArtifactFile() != null) {
            jarFile = gradleProject.getArtifactFile().toPath();
            gradleProject.packageJar();
            if (!gradleProject.getArtifactFile().exists()) {
                final String error = String.format("Failed generate jar file for project(%s)", gradleProject.getName());
                throw new AzureToolkitRuntimeException(error);
            }
            FileUtils.copyFileToDirectory(gradleProject.getArtifactFile(), stagingFolder.toFile());
        } else {
            jarFile = JarUtils.buildJarFileToStagingPath(stagingFolder.toString(), module);
        }

        final String scriptFilePath = "../" + jarFile.getFileName().toString();
        configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
        for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
            if (StringUtils.isNotBlank(config.getKey())) {
                final File functionJsonFile = Paths.get(stagingFolder.toString(), config.getKey(), FUNCTION_JSON)
                        .toFile();
                writeFunctionJsonFile(functionJsonFile, config.getValue());
            }
        }

        final File hostJsonFile = new File(stagingFolder.toFile(), "host.json");
        copyFilesWithDefaultContent(hostJson, hostJsonFile, DEFAULT_HOST_JSON);

        final List<File> dependencies = new ArrayList<>();
        if (gradleProject.isValid()) {
            gradleProject.getDependencies().forEach(lib -> dependencies.add(lib));
        } else {
            OrderEnumerator.orderEntries(module).productionOnly().forEachLibrary(lib -> {
                Arrays.stream(lib.getFiles(OrderRootType.CLASSES)).map(virtualFile -> new File(stripExtraCharacters(virtualFile.getPath())))
                        .filter(File::exists)
                        .forEach(dependencies::add);
                return true;
            });
        }
        final String libraryToExclude = dependencies.stream()
                .filter(artifact -> StringUtils.equalsAnyIgnoreCase(artifact.getName(), AZURE_FUNCTIONS_JAVA_CORE_LIBRARY))
                .map(File::getName).findFirst().orElse(AZURE_FUNCTIONS_JAVA_LIBRARY);

        final File libFolder = new File(stagingFolder.toFile(), "lib");
        for (final File file : dependencies) {
            if (!StringUtils.containsIgnoreCase(file.getName(), libraryToExclude)) {
                FileUtils.copyFileToDirectory(file, libFolder);
            }
        }
        return configMap;
    }

    public static String getTargetFolder(Module module) {
        if (module == null) {
            return StringUtils.EMPTY;
        }
        final Project project = module.getProject();
        final MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
        final String functionAppName = mavenProject == null ? null : mavenProject.getProperties().getProperty(
                "functionAppName");
        final String stagingFolderName = StringUtils.isEmpty(functionAppName) ? module.getName() : functionAppName;
        return Paths.get(project.getBasePath(), "target", "azure-functions", stagingFolderName).toString();
    }

    private static Map<String, FunctionConfiguration> generateConfigurations(final PsiMethod[] methods)
            throws AzureExecutionException {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final PsiMethod method : methods) {
            final PsiAnnotation annotation = AnnotationUtil.findAnnotation(method,
                    FunctionUtils.AZURE_FUNCTION_ANNOTATION_CLASS);
            final String functionName = AnnotationUtil.getDeclaredStringAttributeValue(annotation, "value");
            configMap.put(functionName, generateConfiguration(method));
        }
        return configMap;
    }

    private static FunctionConfiguration generateConfiguration(PsiMethod method) throws AzureExecutionException {
        final FunctionConfiguration config = new FunctionConfiguration();
        final List<Binding> bindings = new ArrayList<>();
        processParameterAnnotations(method, bindings);
        processMethodAnnotations(method, bindings);
        patchStorageBinding(method, bindings);
        config.setEntryPoint(method.getContainingClass().getQualifiedName() + "." + method.getName());
        // Todo: add set bindings method in tools-common
        config.setBindings(bindings);
        return config;
    }

    private static void processParameterAnnotations(final PsiMethod method, final List<Binding> bindings)
            throws AzureExecutionException {
        for (final JvmParameter param : method.getParameters()) {
            bindings.addAll(parseAnnotations(method.getProject(), param.getAnnotations()));
        }
    }

    private static List<Binding> parseAnnotations(final Project project,
                                                  JvmAnnotation[] annotations) throws AzureExecutionException {
        final List<Binding> bindings = new ArrayList<>();

        for (final JvmAnnotation annotation : annotations) {
            final Binding binding = getBinding(project, annotation);
            if (binding != null) {
                Log.debug("Adding binding: " + binding.toString());
                bindings.add(binding);
            }
        }

        return bindings;
    }

    private static Binding getBinding(final Project project, JvmAnnotation annotation) throws AzureExecutionException {
        if (annotation == null) {
            return null;
        }
        if (!(annotation instanceof PsiAnnotation)) {
            throw new AzureExecutionException(
                    AzureBundle.message("function.binding.error.parseFailed",
                            PsiAnnotation.class.getCanonicalName(),
                            annotation.getClass().getCanonicalName()));
        }

        final BindingEnum annotationEnum =
                Arrays.stream(BindingEnum.values())
                        .filter(bindingEnum -> StringUtils.equalsIgnoreCase(bindingEnum.name(),
                                ClassUtils.getShortClassName(annotation.getQualifiedName())))
                        .findFirst()
                        .orElse(null);
        return annotationEnum == null ? getUserDefinedBinding(project, (PsiAnnotation) annotation)
                : createBinding(project, annotationEnum, (PsiAnnotation) annotation);
    }

    private static Binding getUserDefinedBinding(final Project project, PsiAnnotation annotation) throws AzureExecutionException {
        final PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
        if (referenceElement == null) {
            return null;
        }
        final PsiAnnotation customBindingAnnotation =
                AnnotationUtil.findAnnotation((PsiModifierListOwner) referenceElement.resolve(),
                        AZURE_FUNCTION_CUSTOM_BINDING_CLASS);
        if (customBindingAnnotation == null) {
            return null;
        }
        final Map<String, Object> annotationProperties = AnnotationHelper.evaluateAnnotationProperties(project,
                annotation,
                CUSTOM_BINDING_RESERVED_PROPERTIES);
        final Map<String, Object> customBindingProperties = AnnotationHelper.evaluateAnnotationProperties(project,
                customBindingAnnotation,
                null);

        final Map<String, Object> mergedMap = new HashMap<>(annotationProperties);
        customBindingProperties.forEach(mergedMap::putIfAbsent);
        final Binding extendBinding = new Binding(BindingEnum.CustomBinding) {

            public String getName() {
                return (String) mergedMap.get("name");
            }

            public String getDirection() {
                return (String) mergedMap.get("direction");
            }

            public String getType() {
                return (String) mergedMap.get("type");
            }
        };

        annotationProperties.forEach((name, value) -> {
            if (!CUSTOM_BINDING_RESERVED_PROPERTIES.contains(name)) {
                extendBinding.setAttribute(name, value);
            }
        });
        return extendBinding;
    }

    private static void processMethodAnnotations(final PsiMethod method, final List<Binding> bindings)
            throws AzureExecutionException {
        if (!method.getReturnType().equals(Void.TYPE)) {
            bindings.addAll(parseAnnotations(method.getProject(), method.getAnnotations()));

            if (bindings.stream().anyMatch(b -> b.getBindingEnum() == BindingEnum.HttpTrigger) &&
                    bindings.stream().noneMatch(b -> StringUtils.equalsIgnoreCase(b.getName(), "$return"))) {
                bindings.add(getHTTPOutBinding());
            }
        }
    }

    private static void patchStorageBinding(final PsiMethod method, final List<Binding> bindings) {
        final PsiAnnotation storageAccount = AnnotationUtil.findAnnotation(method, STORAGE_ACCOUNT);

        if (storageAccount != null) {
            // todo: Remove System.out.println
            System.out.println(message("function.binding.storage.found"));

            final String connectionString = AnnotationUtil.getDeclaredStringAttributeValue(storageAccount, "value");
            // Replace empty connection string
            bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                    .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                    .forEach(binding -> binding.setAttribute("connection", connectionString));

        } else {
            // todo: Remove System.out.println
            System.out.println(message("function.binding.storage.notFound"));
        }
    }

    private static Binding createBinding(final Project project, BindingEnum bindingEnum, PsiAnnotation annotation)
            throws AzureExecutionException {
        final Binding binding = new Binding(bindingEnum);
        AnnotationHelper.evaluateAnnotationProperties(project, annotation, REQUIRED_ATTRIBUTE_MAP.get(bindingEnum))
                .forEach((name, value) -> {
                    binding.setAttribute(name, value);
                });
        return binding;
    }
}
