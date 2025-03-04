// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application.Parts;
using JetBrains.IDE.UsageStatistics;
using JetBrains.ProjectModel;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class FunctionAppProjectTechnologyProvider : IProjectTechnologyProvider
{
    public IEnumerable<string> GetProjectTechnology(IProject project)
    {
        if (project.IsAzureFunctionsProject())
        {
            yield return "Azure Function";
        }
    }
}