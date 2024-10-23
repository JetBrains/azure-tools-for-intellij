﻿//------------------------------------------------------------------------------
// <auto-generated>
//     Generated by JetBrains ErrorDescriptionGenerator
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Application.I18n;
using JetBrains.Application.Parts;
using JetBrains.Application.Settings;
using JetBrains.Application;
using JetBrains.Core;
using JetBrains.DataFlow;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ReSharper.Daemon;
using JetBrains.ReSharper.Feature.Services.Daemon.Attributes;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi.Resolve;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using JetBrains.ReSharper.Azure.Daemon.Errors;
using JetBrains.ReSharper.Azure.Daemon.FunctionApp.Stages.Analysis;
using JetBrains.ReSharper.Azure.Daemon.Highlighters;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;


namespace JetBrains.ReSharper.Azure.Daemon.Errors.FunctionAppErrors
{
  #region TimerTriggerCronExpressionError

  [ConfigurableSeverityHighlighting(HIGHLIGHTING_ID, "CSHARP", Languages = "CSHARP", OverlapResolve = OverlapResolveKind.NONE, ToolTipFormatString = MESSAGE)]
  public sealed partial class TimerTriggerCronExpressionError : IHighlighting
  {
    protected const string MESSAGE = "{0}";
    public const string HIGHLIGHTING_ID = "Azure.FunctionApp.TimerTriggerCronExpression";

    public TimerTriggerCronExpressionError(ICSharpExpression expression, string cronErrorMessage)
    {
      Expression = expression;
      CronErrorMessage = cronErrorMessage;
      ToolTip = string.Format(MESSAGE, cronErrorMessage).NON_LOCALIZABLE();
    }

    public ICSharpExpression Expression { get; }
    public string CronErrorMessage { get; }

    public /*Localized*/ string ToolTip { get; }
    public /*Localized*/ string ErrorStripeToolTip => ToolTip;

    public DocumentRange CalculateRange()
    {
      return Expression.GetHighlightingRange();
    }

    public bool IsValid()
    {
      return (Expression == null || Expression.IsValid());
    }
  }

  #endregion

#region Configurable Severity Registrar
  [RegisterConfigurableSeverity(TimerTriggerCronExpressionError.HIGHLIGHTING_ID, null, null, null, AzureHighlightingGroupIds.FunctionApp, "Invalid Function App Timer Trigger Cron expression", null, null, "Function App Timer Trigger Cron expression is not valid and can not be used.", null, null, Severity.ERROR)]
  public class RegisterSeverityComponentCAC4631F674C122DAC43AAECF236F252D2734D93052808AE491FDDD72FB80E4C
  {
  }
#endregion
}
