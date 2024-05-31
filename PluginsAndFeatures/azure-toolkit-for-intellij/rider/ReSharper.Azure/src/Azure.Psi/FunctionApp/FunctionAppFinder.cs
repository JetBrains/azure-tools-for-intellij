// Copyright (c) 2020-2024 JetBrains s.r.o.
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

using System;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Metadata.Reader.API;
using JetBrains.Metadata.Reader.Impl;
using JetBrains.Rd.Base;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp
{
    public static class FunctionAppFinder
    {
        private static ILogger ourLogger = Logger.GetLogger(typeof(FunctionAppFinder));

        static class DefaultWorker
        {
            public static readonly IClrTypeName FunctionNameAttributeTypeName =
                new ClrTypeName("Microsoft.Azure.WebJobs.FunctionNameAttribute");

            public static readonly IClrTypeName TimerTriggerAttributeTypeName =
                new ClrTypeName("Microsoft.Azure.WebJobs.TimerTriggerAttribute");
            
            public static readonly IClrTypeName HttpTriggerAttributeTypeName =
                new ClrTypeName("Microsoft.Azure.WebJobs.HttpTriggerAttribute");
        }
        
        static class IsolatedWorker
        {
            public static readonly IClrTypeName FunctionAttributeTypeName =
                new ClrTypeName("Microsoft.Azure.Functions.Worker.FunctionAttribute");

            public static readonly IClrTypeName TimerTriggerAttributeTypeName =
                new ClrTypeName("Microsoft.Azure.Functions.Worker.TimerTriggerAttribute");
            
            public static readonly IClrTypeName HttpTriggerAttributeTypeName =
                new ClrTypeName("Microsoft.Azure.Functions.Worker.HttpTriggerAttribute");
        }

        /// <summary>
        /// Get Function Name from Attribute for a provided method or null if attribute is missing
        /// </summary>
        /// <param name="method">A Method instance to check.</param>
        /// <returns>Function App name string value.</returns>
        [CanBeNull]
        public static string GetFunctionNameFromMethod([CanBeNull] IMethod method)
        {
            if (method == null) return null;

            var functionAttribute = GetFunctionNameAttribute(method);
            if (functionAttribute == null) return null;

            var functionParameters = functionAttribute.PositionParameters().ToArray();
            if (functionParameters.Length < 1)
            {
                ourLogger.Warn($"Insufficient number of parameters in '{functionAttribute.GetAttributeShortName()}' attribute to get a Function name.");
                return null;
            }

            var functionNameParameter = functionParameters.First();

            if (functionNameParameter == null || !functionNameParameter.ConstantValue.IsString())
            {
                ourLogger.Error(
                    $"Unable to get a Function name from '{method.ShortName}' method attribute parameter: '{functionNameParameter.PrintToString()}'.");
                return null;
            }

            return functionNameParameter.ConstantValue.StringValue;
        }
        
        /// <summary>
        /// Get Http Trigger Attribute properties for a provided method's parameters, or null if attribute is missing
        /// </summary>
        /// <param name="method">A Method instance to check.</param>
        /// <returns>Function App Http Trigger Attribute properties.</returns>
        [CanBeNull]
        public static HttpTriggerAttributeProperties GetHttpTriggerAttributeFromMethod([CanBeNull] IMethod method)
        {
            if (method == null) return null;
            if (GetFunctionNameAttribute(method) == null) return null;

            foreach (var methodParameter in method.Parameters)
            {
                var httpTriggerAttribute = GetHttpTriggerAttribute(methodParameter);
                if (httpTriggerAttribute != null)
                {
                    var httpTriggerAttributeProperties = new HttpTriggerAttributeProperties();
                    
                    // Try with positional parameters (known signatures)
                    var positionParameters = httpTriggerAttribute.PositionParameters().ToArray();
                    if (positionParameters.Length == 1 && positionParameters[0].IsArray)
                    {
                        // HttpTriggerAttribute(params string[] methods)
                        httpTriggerAttributeProperties.Methods =
                            positionParameters[0].ArrayValue?.Select(it => it.ConstantValue.StringValue).AsArray();
                    }
                    else if (positionParameters.Length == 1)
                    {
                        // HttpTriggerAttribute(AuthLevel AuthLevel)
                        httpTriggerAttributeProperties.AuthLevel = positionParameters[0].ConstantValue.AsString();
                    }
                    else if (positionParameters.Length == 2 && positionParameters[1].IsArray)
                    {
                        // HttpTriggerAttribute(AuthLevel, params string[] methods)
                        httpTriggerAttributeProperties.AuthLevel = positionParameters[0].ConstantValue.AsString();
                        httpTriggerAttributeProperties.Methods =
                            positionParameters[1].ArrayValue?.Select(it => it.ConstantValue.StringValue).AsArray();
                    }
                    
                    // Try with named parameters
                    foreach (var (name, value) in httpTriggerAttribute.NamedParameters())
                    {
                        if (string.Equals(name, "Route", StringComparison.OrdinalIgnoreCase) && value.IsConstant)
                        {
                            httpTriggerAttributeProperties.Route = value.ConstantValue.StringValue;
                        }
                        else if (string.Equals(name, "Methods", StringComparison.OrdinalIgnoreCase) && value.IsArray)
                        {
                            httpTriggerAttributeProperties.Methods =
                                value.ArrayValue?.Select(it => it.ConstantValue.StringValue).AsArray();
                        }
                        else if (string.Equals(name, "AuthLevel", StringComparison.OrdinalIgnoreCase))
                        {
                            httpTriggerAttributeProperties.AuthLevel = value.ConstantValue.AsString();
                        }
                    }

                    return httpTriggerAttributeProperties;
                }
            }

            return null;
        }

        /// <summary>
        /// Check whether a method define a Function App that can be run.
        /// Reference links:
        /// - https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-class-library#methods-recognized-as-functions
        /// - https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-dependency-injection
        /// </summary>
        /// <param name="method">A Method instance to check.</param>
        /// <returns>Flag whether provided method can be considered as a method to start a Function App of any type.</returns>
        public static bool IsSuitableFunctionAppMethod([CanBeNull] IMethod method)
        {
            return method != null &&
                   method.GetAccessRights() == AccessRights.PUBLIC &&
                   GetFunctionNameAttribute(method) != null;
        }

        /// <summary>
        /// Check whether declared type is a Function App Timer Trigger type.
        /// </summary>
        /// <param name="typeElement">A type element to check.</param>
        /// <returns>Flag whether type element match Function App Timer Trigger attribute type.</returns>
        public static bool IsTimerTriggerAttribute([NotNull] ITypeElement typeElement)
        {
            return typeElement.GetClrName().Equals(DefaultWorker.TimerTriggerAttributeTypeName) ||
                   typeElement.GetClrName().Equals(IsolatedWorker.TimerTriggerAttributeTypeName);
        }

        [CanBeNull]
        private static IAttributeInstance GetFunctionNameAttribute([NotNull] IMethod method)
        {
            var functionAttributes = method.GetAttributeInstances(DefaultWorker.FunctionNameAttributeTypeName, false)
                .Union(method.GetAttributeInstances(IsolatedWorker.FunctionAttributeTypeName, false))
                .ToList();

            if (functionAttributes.IsEmpty())
            {
                if (ourLogger.IsTraceEnabled())
                {
                    ourLogger.Trace(
                        $"Unable to get a proper function name from a method '{method.ShortName}' that has more then one [Function] attribute.");
                }

                return null;
            }

            if (functionAttributes.Count > 1)
            {
                ourLogger.Info(
                    $"Found more then one FunctionName attribute from a method '{method.ShortName}'. Return the first match.");
            }

            return functionAttributes.First();
        }
        
        [CanBeNull]
        private static IAttributeInstance GetHttpTriggerAttribute([NotNull] IParameter parameter)
        {
            var httpTriggerAttributes = parameter.GetAttributeInstances(DefaultWorker.HttpTriggerAttributeTypeName, false)
                .Union(parameter.GetAttributeInstances(IsolatedWorker.HttpTriggerAttributeTypeName, false))
                .ToList();

            if (httpTriggerAttributes.IsEmpty())
            {
                if (ourLogger.IsTraceEnabled())
                {
                    ourLogger.Trace(
                        $"No HttpTriggerAttribute was found for parameter '{parameter.ShortName}'.");
                }

                return null;
            }

            if (httpTriggerAttributes.Count > 1)
            {
                ourLogger.Info(
                    $"Found more than one HttpTriggerAttribute attribute for parameter '{parameter.ShortName}'. Return the first match.");
            }

            return httpTriggerAttributes.First();
        }
    }
}