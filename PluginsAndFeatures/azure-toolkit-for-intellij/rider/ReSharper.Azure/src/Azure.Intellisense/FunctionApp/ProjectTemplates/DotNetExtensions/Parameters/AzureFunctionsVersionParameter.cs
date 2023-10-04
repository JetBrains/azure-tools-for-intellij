// Copyright (c) 2022 JetBrains s.r.o.
//
// All rights reserved.
//
// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// the Software.
//
// THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

using System.Collections.Generic;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetTemplates;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Parameters
{
    internal class AzureFunctionsVersionParameter : DotNetTemplateParameter
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
                foreach (var parameterOptionFromTemplate in parameter.Choices)
                {
                    var presentation = !parameterOptionFromTemplate.Value.DisplayName.IsNullOrWhitespace()
                        ? parameterOptionFromTemplate.Value.DisplayName
                        : parameterOptionFromTemplate.Value.Description;

                    var content = factory.CreateNextParameters(new[] {expander}, index + 1, context);
                    options.Add(new RdProjectTemplateGroupOption(parameterOptionFromTemplate.Key, presentation ?? parameterOptionFromTemplate.Key, null, content));
                }
            }
            else
            {
                // Use hardcoded list
                var isNet6OrHigher = expander.Template.Sdk.Major >= 6 || expander.Template.Sdk.Major == 0; // The Azure Functions templates treat "0" as .NET 6 as well.
                var supportedAzureFunctionsVersions = isNet6OrHigher
                    ? new[] { "v4" }
                    : new[] { "v3", "v2" };
                
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