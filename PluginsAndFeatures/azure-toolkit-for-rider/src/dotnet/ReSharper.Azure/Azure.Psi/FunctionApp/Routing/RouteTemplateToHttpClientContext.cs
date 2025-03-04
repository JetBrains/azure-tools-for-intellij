// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Text;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp.Routing;

internal class RouteTemplateToHttpClientContext
{
    public StringBuilder Builder { get; } = new();
}