// Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using JetBrains.Application.Parts;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.MSBuild;
using JetBrains.ProjectModel.Properties;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Azure.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class FunctionAppDaemonHost
{
    private readonly ISolution _solution;
    private readonly FunctionAppDaemonModel _model;

    public FunctionAppDaemonHost(ISolution solution)
    {
        _solution = solution;

        _model = solution.GetProtocolSolution().GetFunctionAppDaemonModel();
        _model.GetAzureFunctionsVersion.SetSync(GetAzureFunctionsVersionHandler);
        _model.GetAzureFunctionWorkerModel.SetSync(GetAzureFunctionWorkerModel);
    }

    public void RunFunctionApp(string projectFilePath) => _model.RunFunctionApp(
        new FunctionAppRequest(
            projectFilePath: projectFilePath,
            methodName: null,
            functionName: null));

    public void DebugFunctionApp(string projectFilePath) => _model.DebugFunctionApp(
        new FunctionAppRequest(
            projectFilePath: projectFilePath,
            methodName: null,
            functionName: null));

    public void RunFunctionApp(string projectFilePath, string methodName, string functionName) =>
        _model.RunFunctionApp(
            new FunctionAppRequest(
                projectFilePath: projectFilePath,
                methodName: methodName,
                functionName: functionName));

    public void DebugFunctionApp(string projectFilePath, string methodName, string functionName) =>
        _model.DebugFunctionApp(
            new FunctionAppRequest(
                projectFilePath: projectFilePath,
                methodName: methodName,
                functionName: functionName));

    public void TriggerFunctionApp(
        string projectFilePath,
        string methodName,
        string functionName,
        FunctionAppTriggerType triggerType,
        FunctionAppHttpTriggerAttribute? httpTriggerAttribute
    ) =>
        _model.TriggerFunctionApp(
            new FunctionAppTriggerRequest(
                projectFilePath: projectFilePath,
                methodName: methodName,
                functionName: functionName,
                triggerType: triggerType,
                httpTriggerAttribute: httpTriggerAttribute));


    private string? GetAzureFunctionsVersionHandler(Lifetime lifetime, AzureFunctionsVersionRequest request)
    {
        var project = FindProjectByPath(request.ProjectFilePath);
        if (project is null) return null;

        var version = project.GetUniqueRequestedProjectProperty(MSBuildProjectUtil.AzureFunctionsVersionProperty);

        return version;
    }

    private AzureFunctionWorkerModel GetAzureFunctionWorkerModel(
        Lifetime lifetime,
        AzureFunctionWorkerModelRequest request)
    {
        var project = FindProjectByPath(request.ProjectFilePath);
        if (project is null) return AzureFunctionWorkerModel.Unknown;

        var model = project.GetFunctionProjectWorkerModel();

        return model switch
        {
            FunctionProjectWorkerModel.Default => AzureFunctionWorkerModel.Default,
            FunctionProjectWorkerModel.Isolated => AzureFunctionWorkerModel.Isolated,
            FunctionProjectWorkerModel.Unknown => AzureFunctionWorkerModel.Unknown,
            _ => throw new ArgumentOutOfRangeException(nameof(request))
        };
    }

    private IProject? FindProjectByPath(string projectFilePath)
    {
        var path = VirtualFileSystemPath.Parse(projectFilePath, InteractionContext.SolutionContext);
        IProject? project;
        using (ReadLockCookie.Create())
        {
            project = _solution.FindProjectByProjectFilePath(path);
        }

        return project;
    }
}