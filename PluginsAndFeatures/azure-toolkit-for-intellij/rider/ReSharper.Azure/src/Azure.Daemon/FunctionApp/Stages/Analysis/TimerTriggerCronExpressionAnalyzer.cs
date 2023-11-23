// Copyright (c) 2020-2023 JetBrains s.r.o.
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
using CronExpressionDescriptor;
using JetBrains.Annotations;
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

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp.Stages.Analysis
{
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

            var expressionArgument = element.Arguments.First().Value;
            if (!expressionArgument.Type().IsString()) return;

            var literal = (expressionArgument as ICSharpLiteralExpression)?.ConstantValue.StringValue;
            if (literal.IsEmpty()) return;
            
            if (literal.StartsWith("%") && literal.EndsWith("%") && literal.Length > 2) return;

            var mayBeTimeSpanSchedule = literal.Contains(":");
            if (mayBeTimeSpanSchedule)
            {
                if (IsValidTimeSpanSchedule(literal, out var errorMessage, out var description))
                {
                    consumer.AddHighlighting(new TimerTriggerCronExpressionHint(description, expressionArgument, expressionArgument.GetDocumentEndOffset()));
                }
                else if (!string.IsNullOrEmpty(description))
                {
                    consumer.AddHighlighting(new TimerTriggerCronExpressionError(expressionArgument, errorMessage));
                }
            }
            else
            {
                if (IsValidCrontabSchedule(literal, out var errorMessage, out var description))
                {
                    consumer.AddHighlighting(new TimerTriggerCronExpressionHint(description, expressionArgument, expressionArgument.GetDocumentEndOffset()));
                }
                else if (!string.IsNullOrEmpty(description))
                {
                    consumer.AddHighlighting(new TimerTriggerCronExpressionError(expressionArgument, errorMessage));
                }
            }
        }

        private bool IsValidCrontabSchedule(
            string literal,
            [CanBeNull] out string errorMessage,
            [CanBeNull] out string description)
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
                    description = null;
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
        
        private bool IsValidTimeSpanSchedule(string literal, [CanBeNull] out string errorMessage, [CanBeNull] out string description)
        {
            // See https://github.com/Azure/azure-webjobs-sdk-extensions/blob/dev/src/WebJobs.Extensions/Extensions/Timers/Scheduling/TimerSchedule.cs#L77
            try
            {
                // ReSharper disable once ReturnValueOfPureMethodIsNotUsed
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
        private string NormalizeCronSchedule(string cronSchedule)
        {
            var numberOfFields = cronSchedule.Trim()
                .Replace(@"\t", " ")
                .Split(new [] { ' ' }, StringSplitOptions.RemoveEmptyEntries)
                .Length;

            if (numberOfFields == 5)
            {
                return "0 " + cronSchedule;
            }

            return cronSchedule;
        }
    }
}