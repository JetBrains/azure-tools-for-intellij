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
            public AzureFunctionsVersionParameter() : base("AzureFunctionsVersion", "Functions host", "The setting that determines the functions host target
            release")
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
                    foreach (var parameterOptionFromTemplate in parameter.Choices)
                    {
                        var content = factory.CreateNextParameters(new[] {expander}, index + 1, context);
                        options.Add(new RdProjectTemplateGroupOption(parameterOptionFromTemplate.Key, parameterOptionFromTemplate.Value, null, content));
                    }
                }
                else
                {
                    // Use hardcoded list
                    var content = factory.CreateNextParameters(new[] {expander}, index + 1, context);
                    options.Add(new RdProjectTemplateGroupOption("V4", "V4", null, content));
                }
                
                return new RdProjectTemplateGroupParameter(Name,PresentableName, parameter.DefaultValue, Tooltip, options);
            }
        }
    }
}