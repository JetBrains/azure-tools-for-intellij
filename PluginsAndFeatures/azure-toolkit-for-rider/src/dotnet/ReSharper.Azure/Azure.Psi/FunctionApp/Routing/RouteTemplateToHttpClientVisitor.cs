// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.ReSharper.Psi.AspRouteTemplates.Impl.Tree;
using JetBrains.ReSharper.Psi.AspRouteTemplates.Tree;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp.Routing;

internal class RouteTemplateToHttpClientVisitor : RouteTemplateTreeVisitorBase<RouteTemplateToHttpClientContext, RouteTemplateToHttpClientContext>
{
    public override RouteTemplateToHttpClientContext Visit(IRouteSegmentTreeNode segment, RouteTemplateToHttpClientContext context)
    {
        foreach (var part in segment.Parts)
        {
            part.Accept(this, context);
        }
        return context;
    }

    public override RouteTemplateToHttpClientContext Visit(IRouteDelimiterTreeNode delimiter, RouteTemplateToHttpClientContext context)
    {
        context.Builder.Append(delimiter.GetText());
        return context;
    }

    public override RouteTemplateToHttpClientContext Visit(IStaticTextRoutePartTreeNode staticText, RouteTemplateToHttpClientContext context)
    {
        context.Builder.Append(staticText.GetText());
        return context;
    }

    public override RouteTemplateToHttpClientContext Visit(IRouteParameterTreeNode parameter, RouteTemplateToHttpClientContext context)
    {
        if (parameter.Name == null) return context;

        context.Builder.Append("{{");
        context.Builder.Append(parameter.Name.NameValue);
        context.Builder.Append("}}");

        return context;
    }

    public override RouteTemplateToHttpClientContext Visit(IRouteTemplateFile file, RouteTemplateToHttpClientContext context)
    {
        foreach (var part in file.Parts)
        {
            part.Accept(this, context);
        }
        return context;
    }
}