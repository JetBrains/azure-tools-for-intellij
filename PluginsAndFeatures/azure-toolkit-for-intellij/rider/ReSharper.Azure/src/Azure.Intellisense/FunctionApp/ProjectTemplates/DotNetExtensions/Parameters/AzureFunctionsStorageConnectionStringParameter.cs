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

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Parameters
{
    internal class AzureFunctionsStorageConnectionStringParameter : DotNetTemplateParameter
    {
        public AzureFunctionsStorageConnectionStringParameter() : base(
            name: "StorageConnectionStringValue",
            presentableName: "Connection string", 
            tooltip: "The connection string for your Azure WebJobs Storage.")
        {
        }

        public override RdProjectTemplateContent CreateContent(DotNetProjectTemplateExpander expander, IDotNetTemplateContentFactory factory, int index, IDictionary<string, string> context)
        {
            var parameter = expander.TemplateInfo.GetParameter(Name);
            if (parameter == null)
            {
                return factory.CreateNextParameters(new[] {expander}, index + 1, context);
            }

            var content = factory.CreateNextParameters(new[] {expander}, index + 1, context);
            // TODO: RdTextParameterStyle.FileChooser does not support numeric/text only input
            return new RdProjectTemplateTextParameter(Name, PresentableName, parameter.DefaultValue, Tooltip, RdTextParameterStyle.FileChooser, content);
        }
    }
}