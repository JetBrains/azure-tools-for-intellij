// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Context;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Templates;
using JetBrains.UI.ThemedIcons;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;

public class InAzureFunctionsProject : InAnyProject
{
    private static readonly Guid DefaultGuid = new("41A8B8D2-2DE7-43F8-A812-220E5BC95BEB");

    public override Guid GetDefaultUID() => DefaultGuid;
    public override string PresentableShortName => "Azure Functions projects";
}

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureProjectScopeProvider : ScopeProvider
{
    static AzureProjectScopeProvider()
    {
        TemplateImage.Register("AzureFunctionsTrigger", FunctionAppTemplatesThemedIcons.AzureFunctionsTrigger.Id);
    }

    public AzureProjectScopeProvider()
    {
        Creators.Add(TryToCreate<InAzureFunctionsProject>);
        Creators.Add(TryToCreate<MustUseAzureFunctionsDefaultWorker>);
        Creators.Add(TryToCreate<MustUseAzureFunctionsIsolatedWorker>);
    }

    public override IEnumerable<ITemplateScopePoint> ProvideScopePoints(TemplateAcceptanceContext context)
    {
        var project = context.GetProject();
        if (project == null || !project.IsAzureFunctionsProject()) yield break;

        yield return new InAzureFunctionsProject();

        if (project.HasDefaultWorkerPackageReference(null))
            yield return new MustUseAzureFunctionsDefaultWorker();

        if (project.HasIsolatedWorkerPackageReference(null))
            yield return new MustUseAzureFunctionsIsolatedWorker();

        foreach (var scope in GetLanguageSpecificScopePoints(project))
        {
            yield return scope;
        }
    }

    protected virtual IEnumerable<ITemplateScopePoint> GetLanguageSpecificScopePoints(IProject project)
    {
        yield break;
    }
}

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class MustUseAzureFunctionsDefaultWorker : InAzureFunctionsProject, IMandatoryScopePoint
{
    private static readonly Guid OurDefaultGuid = new("1E429A05-3577-4B43-86FD-8EC8AF9C877F");

    public override Guid GetDefaultUID() => OurDefaultGuid;
    public override string PresentableShortName => "Azure Functions with Default Worker";
}

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class MustUseAzureFunctionsIsolatedWorker : InAzureFunctionsProject, IMandatoryScopePoint
{
    private static readonly Guid OurDefaultGuid = new("081BD100-484A-4FB2-AD24-B2EC16E68547");

    public override Guid GetDefaultUID() => OurDefaultGuid;
    public override string PresentableShortName => "Azure Functions with Isolated Worker";
}