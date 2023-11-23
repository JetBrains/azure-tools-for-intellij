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

using JetBrains.DocumentModel;
using JetBrains.ReSharper.Daemon;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Feature.Services.InlayHints;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.TextControl.DocumentMarkup;
using JetBrains.TextControl.DocumentMarkup.VisualStudio;
using JetBrains.UI.RichText;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp.InlayHints
{
    [RegisterHighlighter(
        HighlightAttributeId,
        ForegroundColor = "#707070",
        BackgroundColor = "#EBEBEB",
        DarkForegroundColor = "#787878",
        DarkBackgroundColor = "#3B3B3C",
        EffectType = EffectType.INTRA_TEXT_ADORNMENT,
        Layer = HighlighterLayer.ADDITIONAL_SYNTAX,
        VsGenerateClassificationDefinition = VsGenerateDefinition.VisibleClassification,
        VsBaseClassificationType = VsPredefinedClassificationType.Text,
        TransmitUpdates = true)]
    [DaemonAdornmentProvider(typeof(TimerTriggerCronExpressionAdornmentProvider))]
    [DaemonTooltipProvider(typeof(InlayHintTooltipProvider))]
    [StaticSeverityHighlighting(Severity.INFO, typeof(HighlightingGroupIds.CodeInsights), AttributeId = HighlightAttributeId)]
    public class TimerTriggerCronExpressionHint : IInlayHintWithDescriptionHighlighting
    {
        public const string HighlightAttributeId = nameof(TimerTriggerCronExpressionHint);

        private readonly string _description;
        private readonly ITreeNode _node;
        private readonly DocumentOffset _offset;

        public TimerTriggerCronExpressionHint(string description, ITreeNode node, DocumentOffset offset)
        {
            _description = description;
            _node = node;
            _offset = offset;
        }

        public RichText Description => _description;

        public string ToolTip => _description;

        public string ErrorStripeToolTip => _description;

        public bool IsValid()
        {
            return _node.IsValid();
        }

        public DocumentRange CalculateRange()
        {
            return new DocumentRange(_offset);
        }
    }
}