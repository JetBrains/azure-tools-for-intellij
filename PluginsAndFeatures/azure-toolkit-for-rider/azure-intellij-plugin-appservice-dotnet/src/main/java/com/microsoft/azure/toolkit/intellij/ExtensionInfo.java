/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij;

import lombok.Data;

import java.util.List;

@Data
public class ExtensionInfo {
    public String id;
    public String title;
    public String type;
    public String summary;
    public String description;
    public String version;
    public String extensionUrl;
    public String projectUrl;
    public String iconUrl;
    public String licenseUrl;
    public String feedUrl;
    public List<String> authors;
    public String installerCommandLineParams;
    public String publishedDateTime;
    public int downloadCount;
    public boolean localIsLatestVersion;
    public String localPath;
    public String installedDateTime;
    public String provisioningState;
    public String comment;
    public String packageUri;
}

