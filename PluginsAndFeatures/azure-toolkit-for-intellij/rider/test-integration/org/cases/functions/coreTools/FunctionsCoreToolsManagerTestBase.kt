/**
 * Copyright (c) 2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.cases.functions.coreTools

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeNull
import com.jetbrains.rider.test.asserts.shouldContains
import com.jetbrains.rider.test.asserts.shouldNotBeNull
import com.jetbrains.rider.test.base.BaseTestWithSolution
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsConstants
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsManager
import org.testng.annotations.*
import java.io.File

abstract class FunctionsCoreToolsManagerTestBase(
        private val solutionDirectoryName: String,
        private val azureFunctionsVersion: String
) : BaseTestWithSolution() {

    companion object {
        private const val releaseFeedUrl = "https://functionscdn.azureedge.net/public/cli-feed-v4.json"
    }

    private val tempDir: File by lazy {
        val dir = FileUtil.getTempDirectory().toIOFile()
        val toCreate = dir.resolve(javaClass.simpleName)
        if (!toCreate.exists()) {
            toCreate.mkdirs()
            toCreate.deleteOnExit()
        }
        toCreate
    }

    private val azureTestCoreToolsDownloadDirectory: File by lazy {
        val toCreate = tempDir.resolve("azure-test-core-tools")
        if (!toCreate.exists()) {
            toCreate.mkdirs()
            toCreate.deleteOnExit()
        }
        toCreate
    }

    override fun getSolutionDirectoryName(): String = solutionDirectoryName

    override val waitForCaches: Boolean = true

    override val restoreNuGetPackages: Boolean = true

    private var originalAzureCoreToolsDownloadDirectory: String = ""
    private var originalAzureCoreToolsPathEntries: List<AzureRiderSettings.AzureCoreToolsPathEntry> = emptyList()

    @BeforeMethod(alwaysRun = true)
    fun setUp() {
        val properties = PropertiesComponent.getInstance()

        originalAzureCoreToolsDownloadDirectory = properties.getValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH,
                AzureRiderSettings.VALUE_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH)

        originalAzureCoreToolsPathEntries = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)
    }

    @AfterMethod(alwaysRun = true)
    fun tearDown() {
        val properties = PropertiesComponent.getInstance()

        properties.setValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH,
                originalAzureCoreToolsDownloadDirectory)

        AzureRiderSettings.setAzureCoreToolsPathEntries(properties, originalAzureCoreToolsPathEntries)

        if (tempDir.exists())
            tempDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
    }

    @Test
    fun testDemandCoreToolsPathForVersion_DoNotAllowDownload_ReturnsNull() {

        configureCoreToolsPathEntriesManagedByRider()

        val result = FunctionsCoreToolsManager.demandCoreToolsPathForVersion(
                project, azureFunctionsVersion, releaseFeedUrl, allowDownload = false)
            ?.absolutePath

        result.shouldBeNull("Path should be null when downloading tools is not allowed.")
    }

    @Test
    fun testDemandCoreToolsPathForVersion_AllowDownload_ReturnsNonEmptyPath() {

        configureCoreToolsPathEntriesManagedByRider()

        val result = FunctionsCoreToolsManager.demandCoreToolsPathForVersion(
                project, azureFunctionsVersion, releaseFeedUrl, allowDownload = true)
            ?.absolutePath

        result.shouldNotBeNull("Path should not be null when downloading tools is allowed.")
        result!!.shouldContains(File.separator + azureFunctionsVersion + File.separator)
    }

    @Test
    fun testDemandCoreToolsPathForVersion_DoNotAllowDownload_WithExistingDownload_ReturnsNonEmptyPath() {

        configureCoreToolsPathEntriesManagedByRider()

        // Create fake existing download path
        val existingDownloadPath = azureTestCoreToolsDownloadDirectory
                .resolve(azureFunctionsVersion)
                .resolve("1.2.3")
        if (!existingDownloadPath.exists()) {
            existingDownloadPath.mkdirs()
            existingDownloadPath.deleteOnExit()

            val dummyFunc = existingDownloadPath.resolve("func")
            dummyFunc.createNewFile()
            dummyFunc.deleteOnExit()
        }

        val result = FunctionsCoreToolsManager.demandCoreToolsPathForVersion(
                project, azureFunctionsVersion, releaseFeedUrl, allowDownload = false)
            ?.absolutePath

        result.shouldNotBeNull("Path should not be null.")
        result!!.shouldBe(existingDownloadPath.path)
    }

    private fun configureCoreToolsPathEntriesManagedByRider() {
        val properties = PropertiesComponent.getInstance()

        properties.setValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH,
                azureTestCoreToolsDownloadDirectory.path)

        val entries = FunctionsCoreToolsConstants.FUNCTIONS_CORETOOLS_KNOWN_SUPPORTED_VERSIONS.map {
            AzureRiderSettings.AzureCoreToolsPathEntry(it, "")
        }

        AzureRiderSettings.setAzureCoreToolsPathEntries(properties, entries)
    }
}
