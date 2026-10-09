//package com.microsoft.azure.toolkit.intellij.appservice.task;
//
//import com.intellij.openapi.project.Project;
//import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
//import com.microsoft.azure.toolkit.ide.guidance.Course;
//import com.microsoft.azure.toolkit.ide.guidance.Task;
//import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.StartStreamingLogsAction;
//import com.microsoft.azure.toolkit.lib.Azure;
//import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
//import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
//import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
//
//import javax.annotation.Nonnull;
//
//public class OpenLogStreamingTask implements Task {
//    public static final String RESOURCE_ID = "resource_id";
//    private final Project project;
//    private final Course guidance;
//    private final ComponentContext context;
//
//    public OpenLogStreamingTask(@Nonnull ComponentContext context) {
//        this.context = context;
//        this.project = context.getProject();
//        this.guidance = context.getCourse();
//    }
//
//    @Override
//    @AzureOperation(name = "user/guidance.open_log_streaming")
//    public void execute() throws Exception {
//        final String resourceId = (String) context.getParameter(RESOURCE_ID);
//        final Object resource = Azure.az(AzureAppService.class).getById(resourceId);
//        if (resource instanceof AppServiceAppBase) {
//            new StartStreamingLogsAction((AppServiceAppBase<?, ?, ?>) resource, project).execute();
//        }
//    }
//
//    @Nonnull
//    @Override
//    public String getName() {
//        return "task.app_service.open_log_streaming";
//    }
//}
