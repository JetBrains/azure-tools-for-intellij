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

package org.jetbrains.plugins.azure.functions.coreTools

import com.jetbrains.rider.test.asserts.*
import org.testng.annotations.Test

class AzureFunctionsCoreToolsReleaseServiceTests {

    // Note: This test really serves to make sure expected values are present on the remote URL,
    // and that we can properly deserialize it.
    @Test
    fun testHasExpectedTagsAndReleases() {
        val feed = FunctionsCoreToolsReleaseFeedService.createInstance()
                .getReleaseFeed("https://functionscdn.azureedge.net/public/cli-feed-v4.json")
                .execute()
                .body()

        feed.shouldNotBeNull()
        feed!!.tags.size.shouldNotBe(0)
        feed.releases.size.shouldNotBe(0)

        val osArchitecturesToVerify = mapOf(
                "v2" to mapOf(
                        "Linux" to "x64",
                        "MacOS" to "x64",
                        "Windows" to "x86",
                        "Windows" to "x64"
                ),
                "v3" to mapOf(
                        "Linux" to "x64",
                        "MacOS" to "x64",
                        "Windows" to "x86",
                        "Windows" to "x64"
                ),
                "v4" to mapOf(
                        "Linux" to "x64",
                        "MacOS" to "x64",
                        "Windows" to "x86",
                        "Windows" to "x64"
                ))

        for (tagToOsArchitecture in osArchitecturesToVerify) {
            val tag = feed.tags[tagToOsArchitecture.key]

            tag.shouldNotBeNull()

            val release = feed.releases[tag!!.release]

            release.shouldNotBeNull()

            release!!.templates.shouldNotBeNull()
            release.coreTools.shouldNotBeNull()
            release.coreTools.size.shouldNotBe(0)

            for (osToArchitecture in tagToOsArchitecture.value) {
                val coreTools = release.coreTools.firstOrNull {
                    it.os.equals(osToArchitecture.key, true) && it.architecture.equals(osToArchitecture.value, true) }

                coreTools.shouldNotBeNull()
                coreTools!!.downloadLink.shouldNotBeNull()
            }
        }
    }
}
