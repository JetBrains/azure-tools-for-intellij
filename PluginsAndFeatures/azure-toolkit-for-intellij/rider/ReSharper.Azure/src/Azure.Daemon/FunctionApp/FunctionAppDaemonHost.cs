// Copyright (c) 2020-2022 JetBrains s.r.o.
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

using System.Linq;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.MSBuild;
using JetBrains.ProjectModel.Properties;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Azure.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp
{
    [SolutionComponent]
    public class FunctionAppDaemonHost
    {
        private readonly ISolution _solution;
        private readonly FunctionAppDaemonModel _model;

        public FunctionAppDaemonHost(ISolution solution)
        {
            _solution = solution;
            
            _model = solution.GetProtocolSolution().GetFunctionAppDaemonModel();
            _model.GetAzureFunctionsVersion.Set(GetAzureFunctionsVersionHandler);
        }
        
        public void RunFunctionApp(string methodName, string functionName, string projectFilePath)
        {
            _model.RunFunctionApp(new FunctionAppRequest(methodName, functionName, projectFilePath));
        }

        public void DebugFunctionApp(string methodName, string functionName, string projectFilePath)
        {
            _model.DebugFunctionApp(new FunctionAppRequest(methodName, functionName, projectFilePath));
        }

        public void TriggerFunctionApp(string methodName, string functionName, string projectFilePath)
        {
            _model.TriggerFunctionApp(new FunctionAppRequest(methodName, functionName, projectFilePath));
        }
        
        private RdTask<string> GetAzureFunctionsVersionHandler(Lifetime lifetime, AzureFunctionsVersionRequest request)
        {
            using (ReadLockCookie.Create())
            {
                var project = _solution.FindProjectByProjectFilePath(VirtualFileSystemPath.Parse(request.ProjectFilePath, InteractionContext.SolutionContext));

                var azureFunctionsVersion = project?.GetRequestedProjectProperties(MSBuildProjectUtil.AzureFunctionsVersionProperty).FirstOrDefault();

                return RdTask<string>.Successful(azureFunctionsVersion);
            }
        }
    }
}
