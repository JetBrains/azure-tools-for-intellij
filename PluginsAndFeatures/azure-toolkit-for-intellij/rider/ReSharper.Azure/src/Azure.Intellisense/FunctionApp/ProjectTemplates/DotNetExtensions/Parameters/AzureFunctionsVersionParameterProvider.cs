using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetTemplates;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Parameters
{
    [ShellComponent]
    public class AzureFunctionsVersionParameterProvider : IDotNetTemplateParameterProvider
    {
        public int Priority => 20;

        public IReadOnlyCollection<DotNetTemplateParameter> Get()
        {
            return new[] {new AzureFunctionsVersionParameter()};
        }

        private class AzureFunctionsVersionParameter : DotNetTemplateParameter
        {
            public AzureFunctionsVersionParameter() : base(
                    name: "AzureFunctionsVersion",
                    presentableName: "Functions host", 
                    tooltip: "The setting that determines the functions host target release")
            {
            }

            public override RdProjectTemplateContent CreateContent(DotNetProjectTemplateExpander expander, IDotNetTemplateContentFactory factory, int index, IDictionary<string, string> context)
            {
                var parameter = expander.TemplateInfo.GetParameter(Name);
                if (parameter == null)
                {
                    return factory.CreateNextParameters(new[] {expander}, index + 1, context);
                }

                var options = new List<RdProjectTemplateGroupOption>();
                if (parameter.Choices != null && !parameter.Choices.IsEmpty())
                {
                    // Use from template if provided
                    var content = factory.CreateNextParameters(new[] {expander}, index + 1, context);
                    foreach (var parameterOptionFromTemplate in parameter.Choices)
                    {
                        var presentation = !parameterOptionFromTemplate.Value.DisplayName.IsNullOrWhitespace()
                            ? parameterOptionFromTemplate.Value.DisplayName
                            : parameterOptionFromTemplate.Value.Description;

                        options.Add(new RdProjectTemplateGroupOption(parameterOptionFromTemplate.Key, presentation ?? parameterOptionFromTemplate.Key, null, content));
                    }
                }
                else
                {
                    // Use hardcoded list
                    var isNet6OrHigher = expander.Template.Sdk.Major >= 6 || expander.Template.Sdk.Major == 0; // The Azure Functions templates treat "0" as .NET 6 as well.
                    var supportedAzureFunctionsVersions = isNet6OrHigher
                        ? new[] { "V4" }
                        : new[] { "V3", "V2" };
                    
                    foreach (var version in supportedAzureFunctionsVersions)
                    {
                        var content = factory.CreateNextParameters(new[] {expander}, index + 1, context);
                        options.Add(new RdProjectTemplateGroupOption(version, version, null, content));
                    }
                }
                
                return new RdProjectTemplateGroupParameter(Name,PresentableName, parameter.DefaultValue, Tooltip, options);
            }
        }
    }
}