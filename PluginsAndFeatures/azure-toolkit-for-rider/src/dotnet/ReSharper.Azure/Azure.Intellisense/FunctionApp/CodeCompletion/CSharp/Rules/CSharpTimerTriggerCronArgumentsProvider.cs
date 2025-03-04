// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.DocumentModel;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Feature.Services.CodeCompletion;
using JetBrains.ReSharper.Feature.Services.CodeCompletion.Infrastructure;
using JetBrains.ReSharper.Feature.Services.CodeCompletion.Infrastructure.AspectLookupItems.BaseInfrastructure;
using JetBrains.ReSharper.Feature.Services.CodeCompletion.Infrastructure.AspectLookupItems.Info;
using JetBrains.ReSharper.Feature.Services.CodeCompletion.Infrastructure.AspectLookupItems.Matchers;
using JetBrains.ReSharper.Feature.Services.CodeCompletion.Infrastructure.LookupItems;
using JetBrains.ReSharper.Feature.Services.CSharp.CodeCompletion.Infrastructure;
using JetBrains.ReSharper.Features.Intellisense.CodeCompletion.CSharp;
using JetBrains.ReSharper.Features.Intellisense.CodeCompletion.CSharp.Rules;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.CSharp.Util;
using JetBrains.ReSharper.Psi.Tree;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.CodeCompletion.CSharp.Rules;

[Language(typeof(CSharpLanguage))]
public class CSharpTimerTriggerCronArgumentsProvider : CSharpItemsProviderBase<CSharpCodeCompletionContext>
{
    private readonly struct CronSuggestion(string expression, string description)
    {
        public readonly string Expression = expression;
        public readonly string Description = description;
    }

    private readonly CronSuggestion[] _cronSuggestions =
    [
        new("* * * * * *", "Every second"),
        new("0 * * * * *", "Every minute"),
        new("0 */5 * * * *", "Every 5 minutes"),
        new("0 0 * * * *", "Every hour"),
        new("0 0 */6 * * *", "Every 6 hours at minute 0"),
        new("0 0 8-18 * * *", "Every hour between 08:00 AM and 06:59 PM"),
        new("0 0 0 * * *", "At 12:00 AM"),
        new("0 0 10 * * *", "At 10:00 AM"),
        new("0 0 * * * 1-5", "Every hour, Monday through Friday"),
        new("0 0 0 * * 0", "At 12:00 AM, only on Sunday"),
        new("0 0 9 * * Mon", "At 09:00 AM, only on Monday"),
        new("0 0 0 1 * *", "At 12:00 AM, on day 1 of the month"),
        new("0 0 0 1 1 *", "At 12:00 AM, on day 1 of the month, only in January"),
        new("0 0 * * * Sun", "Every hour, only on Sunday"),
        new("0 0 0 * * Sat,Sun", "At 12:00 AM, only on Saturday and Sunday"),
        new("0 0 0 * * 6,0", "At 12:00 AM, only on Saturday and Sunday"),
        new("0 0 0 1-7 * Sun", "At 12:00 AM, between day 1 and 7 of the month, only on Sunday"),
        new("11 5 23 * * *", "At 11:05:11 PM"),
        new("*/15 * * * * *", "Every 15 seconds"),
        new("0 30 9 * Jan Mon", "At 09:30 AM, only on Monday, only in January")
    ];

    protected override bool IsAvailable(CSharpCodeCompletionContext context)
    {
        return IsStringLiteral(context) &&
               IsTimerTriggerAnnotation(context) &&
               context.BasicContext.CodeCompletionType == CodeCompletionType.BasicCompletion;
    }

    protected override bool AddLookupItems(CSharpCodeCompletionContext context, IItemsCollector collector)
    {
        foreach (var cronSuggestion in _cronSuggestions)
        {
            if (context.NodeInFile.Parent is not ICSharpLiteralExpression literalExpression) return false;
            var ranges = GetRangeWithinQuotes(literalExpression);

            var lookupItem =
                CSharpLookupItemFactory.Instance.CreateTextLookupItem(new TextLookupRanges(ranges, ranges),
                    cronSuggestion.Expression);

            lookupItem.Presentation.DisplayTypeName.Text = cronSuggestion.Description;

            lookupItem.WithMatcher(static item =>
                new TextualMatcher<TextualInfo>(item.Info.Text.Replace(" ", "·"), item.Info));

            collector.Add(lookupItem);
        }

        return true;
    }

    private static DocumentRange GetRangeWithinQuotes(ICSharpLiteralExpression expression)
    {
        var literalAlterer = CSharpStringLiteralAlterer.CreateByLiteralExpression(expression);
        var underQuotesRange = literalAlterer.UnderQuotesRange;
        var containingFile = expression.GetContainingFile();
        return containingFile?.GetDocumentRange(underQuotesRange) ?? DocumentRange.InvalidRange;
    }

    private static bool IsStringLiteral(CSharpCodeCompletionContext context) =>
        context.UnterminatedContext.TreeNode is ITokenNode token &&
        token.GetTokenType().IsStringLiteral;

    private static bool IsTimerTriggerAnnotation(CSharpCodeCompletionContext context)
    {
        var token = context.UnterminatedContext.TreeNode as ITokenNode;
        if (token == null) return false;

        var literal = token.Parent as ICSharpLiteralExpression;
        var argument = CSharpArgumentNavigator.GetByValue(literal);
        var attribute = AttributeNavigator.GetByArgument(argument);

        if (attribute == null) return false;
        if (attribute.Arguments.Count != 1) return false;

        var resolveResult = attribute.TypeReference?.Resolve();
        return resolveResult?.DeclaredElement is ITypeElement typeElement &&
               FunctionAppFinder.IsTimerTriggerAttribute(typeElement);
    }
}