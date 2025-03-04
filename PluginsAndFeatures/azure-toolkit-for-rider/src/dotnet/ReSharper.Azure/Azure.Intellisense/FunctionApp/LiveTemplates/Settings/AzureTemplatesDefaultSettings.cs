// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using System.IO;
using System.Reflection;
using JetBrains.Application;
using JetBrains.Application.Parts;
using JetBrains.Application.Settings;
using JetBrains.Lifetimes;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Settings;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Settings;

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureTemplatesDefaultSettings : IHaveDefaultSettingsStream, IDefaultSettingsRootKey<LiveTemplatesSettings>
{
    public Stream GetDefaultSettingsStream(Lifetime lifetime)
    {
        var stream = Assembly
            .GetExecutingAssembly()
            .GetManifestResourceStream("JetBrains.ReSharper.Azure.Templates.templates.dotSettings");
        ArgumentNullException.ThrowIfNull(stream);
        lifetime.AddDispose(stream);
        return stream;
    }

    public string Name => "Azure default LiveTemplates";
}