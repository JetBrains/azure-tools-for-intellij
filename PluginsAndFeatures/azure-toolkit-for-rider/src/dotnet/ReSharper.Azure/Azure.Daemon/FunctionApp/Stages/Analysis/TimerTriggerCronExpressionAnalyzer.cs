// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using CronExpressionDescriptor;
using JetBrains.ReSharper.Azure.Daemon.Errors.FunctionAppErrors;
using JetBrains.ReSharper.Azure.Daemon.FunctionApp.InlayHints;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using Microsoft.CodeAnalysis.Elfie.Extensions;
using NCrontab;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp.Stages.Analysis;

/// <summary>
/// Analyzer for Cron expressions in Function App Timer Trigger
/// matching NCRONTAB specification, or System.TimeSpan.
///
/// NCRONTAB specification. https://github.com/atifaziz/NCrontab
///
/// In general, NCRONTAB expressions are as follows:
///
/// * * * * * *
/// - - - - - -
/// | | | | | |
/// | | | | | +--- day of week (0 - 6) (Sunday=0)
/// | | | | +----- month (1 - 12)
/// | | | +------- day of month (1 - 31)
/// | | +--------- hour (0 - 23)
/// | +----------- min (0 - 59)
/// +------------- sec (0 - 59)
///
/// Note that NCRONTAB uses 6 fields, but Azure Functions also supports 5 fields.
/// https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-timer?tabs=csharp#ncrontab-expressions
/// </summary>
[ElementProblemAnalyzer(typeof(IAttribute), HighlightingTypes = new[]
{
    typeof(TimerTriggerCronExpressionError),
    typeof(TimerTriggerCronExpressionHint)
})]
public class TimerTriggerCronExpressionAnalyzer : ElementProblemAnalyzer<IAttribute>
{
    protected override void Run(IAttribute element, ElementProblemAnalyzerData data, IHighlightingConsumer consumer)
    {
        if (element.Arguments.Count != 1) return;

        var resolveResult = element.TypeReference?.Resolve();
        var typeElement = resolveResult?.DeclaredElement as ITypeElement;
        if (typeElement == null) return;

        if (!FunctionAppFinder.IsTimerTriggerAttribute(typeElement)) return;

        var expressionArgument = element.Arguments.FirstOrDefault()?.Value;
        if (expressionArgument is null || !expressionArgument.Type().IsString()) return;

        if (expressionArgument is not ICSharpLiteralExpression literalExpression) return;

        var literal = literalExpression.ConstantValue.StringValue;
        if (literal.IsEmpty()) return;
        if (literal.StartsWith('%') && literal.EndsWith('%') && literal.Length > 2) return;

        var mayBeTimeSpanSchedule = literal.Contains(':');
        if (mayBeTimeSpanSchedule)
        {
            if (IsValidTimeSpanSchedule(literal, out var errorMessage, out var description) &&
                !string.IsNullOrEmpty(description))
            {
                consumer.AddHighlighting(new TimerTriggerCronExpressionHint(description, literalExpression,
                    literalExpression.GetDocumentEndOffset()));
            }
            else
            {
                consumer.AddHighlighting(new TimerTriggerCronExpressionError(literalExpression, errorMessage));
            }
        }
        else
        {
            if (IsValidCrontabSchedule(literal, out var errorMessage, out var description) &&
                !string.IsNullOrEmpty(description))
            {
                consumer.AddHighlighting(new TimerTriggerCronExpressionHint(description, literalExpression,
                    literalExpression.GetDocumentEndOffset()));
            }
            else
            {
                consumer.AddHighlighting(new TimerTriggerCronExpressionError(literalExpression, errorMessage));
            }
        }
    }

    private static bool IsValidCrontabSchedule(string literal, out string? errorMessage, out string? description)
    {
        try
        {
            var normalizedLiteral = NormalizeCronSchedule(literal);
            CrontabSchedule.Parse(normalizedLiteral, new CrontabSchedule.ParseOptions { IncludingSeconds = true });
            errorMessage = null;
            try
            {
                description = ExpressionDescriptor.GetDescription(normalizedLiteral);
            }
            catch (Exception)
            {
                description = "";
            }

            return true;
        }
        catch (CrontabException e)
        {
            errorMessage = e.Message;
            description = null;
            return false;
        }
    }

    private static bool IsValidTimeSpanSchedule(string literal, out string? errorMessage, out string? description)
    {
        // See https://github.com/Azure/azure-webjobs-sdk-extensions/blob/dev/src/WebJobs.Extensions/Extensions/Timers/Scheduling/TimerSchedule.cs#L77
        try
        {
            var timeSpan = TimeSpan.Parse(literal);
            errorMessage = null;
            description = $"Every {timeSpan.ToFriendlyString()}";
            return true;
        }
        catch (FormatException e)
        {
            errorMessage = e.Message;
            description = null;
            return false;
        }
    }

    /// <summary>
    /// NCRONTAB expression supports both five field and six field format.
    /// The sixth field position is a value for seconds which is placed at the beginning of the expression.
    ///
    /// This method converts a five field format to a six field format by prepending "0 " when needed.
    /// </summary>
    /// <param name="cronSchedule"></param>
    /// <returns></returns>
    private static string NormalizeCronSchedule(string cronSchedule)
    {
        var numberOfFields = cronSchedule.Trim()
            .Replace(@"\t", " ")
            .Split([' '], StringSplitOptions.RemoveEmptyEntries)
            .Length;

        if (numberOfFields == 5)
        {
            return "0 " + cronSchedule;
        }

        return cronSchedule;
    }
}