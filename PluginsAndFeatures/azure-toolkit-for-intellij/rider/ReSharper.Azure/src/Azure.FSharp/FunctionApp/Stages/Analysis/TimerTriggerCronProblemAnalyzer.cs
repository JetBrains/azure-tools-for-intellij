// Copyright (c) 2020 JetBrains s.r.o.
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

using JetBrains.ReSharper.Azure.FSharp.Errors.FunctionAppErrors;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Plugins.FSharp.Psi.Impl.Tree;
using JetBrains.ReSharper.Plugins.FSharp.Psi.Tree;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using NCrontab;

namespace JetBrains.ReSharper.Azure.FSharp.FunctionApp.Stages.Analysis
{
    /// <summary>
    /// Analyzer for Cron expressions in Function App Timer Trigger matching NCRONTAB specification.
    /// https://github.com/atifaziz/NCrontab
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
    /// </summary>
    [ElementProblemAnalyzer(typeof(IAttribute), HighlightingTypes = new[]
    {
        typeof(TimerTriggerCronExpressionError)
    })]
    public class TimerTriggerCronProblemAnalyzer : ElementProblemAnalyzer<IAttribute>
    {
        protected override void Run(IAttribute element, ElementProblemAnalyzerData data, IHighlightingConsumer consumer)
        {
            if (element.Arguments.Count != 1) return;

            var resolveResult = element.ReferenceName.Reference.Resolve();
            var typeElement = resolveResult.DeclaredElement as ITypeElement;
            if (typeElement == null) return;

            if (!FunctionAppFinder.IsTimerTriggerAttribute(typeElement)) return;

            var expressionArgument = element.Arguments[0].Expression;
            if (expressionArgument == null || !expressionArgument.Type().IsString()) return;

            var fSharpString = (expressionArgument as ILiteralExpr)?.Literal as FSharpString;
            var literal = fSharpString?.ConstantValue.Value as string;
            if (literal.IsEmpty()) return;

            literal = literal.RemoveQuotes();
            
            if (literal.StartsWith("%") && literal.EndsWith("%") && literal.Length > 2) return;

            try
            {
                CrontabSchedule.Parse(literal, new CrontabSchedule.ParseOptions {IncludingSeconds = true});
            }
            catch (CrontabException e)
            {
                consumer.AddHighlighting(new TimerTriggerCronExpressionError(expressionArgument, e.Message));
            }
        }
    }
}
