// Copyright (c) 2024 JetBrains s.r.o.
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

using JetBrains.Annotations;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp
{
    /// <summary>
    /// Properties commonly found on an Azure Functions HttpTriggerAttribute
    /// </summary>
    public class HttpTriggerAttributeProperties
    {
        /// <summary>
        /// Gets or sets the route template for the function. Can include
        /// route parameters using ASP.NET Core supported syntax. If not specified,
        /// will default to the function name.
        /// </summary>
        [CanBeNull]
        public string Route { get; set; }

        /// <summary>
        /// Gets the HTTP methods that are supported for the function.
        /// </summary>
        [CanBeNull]
        public string[] Methods { get; set; }

        /// <summary>
        /// Gets the authorization level for the function.
        /// </summary>
        [CanBeNull]
        public string AuthLevel { get; set; }
    }
}