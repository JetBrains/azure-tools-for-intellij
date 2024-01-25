/**
 * Copyright (c) 2020-2022 JetBrains s.r.o.
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

package org.cases.markup.functionapp.csharp.v4.defaultWorker

import com.jetbrains.rider.test.annotations.Mute
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.env.enums.SdkVersion
import org.cases.markup.functionapp.csharp.AzureFunctionRunMarkerTestBase
import org.testng.annotations.Test

@TestEnvironment(sdkVersion = SdkVersion.DOT_NET_6)
@Mute
class AzureFunctionRunMarkerTest : AzureFunctionRunMarkerTestBase(
        solutionDirectoryName = "v4/FunctionApp",
        testFilePath = "FunctionApp/Function.cs",
        sourceFileName = "Function.cs",
        goldFileName = "Function.gold"
) {

    @Test(description = "Check Http Trigger function having multiple attribute including required [FunctionName] is shown with Function App gutter mark.")
    fun testFunctionApp_HttpTriggerWithMultipleAttributes_Detected() = verifyLambdaGutterMark()

    @Test(description = "Check multiple functions inside one class with [FunctionName] required attribute are shown with gutter mark.")
    fun testFunctionApp_MultipleFunctionApps_Detected() = verifyLambdaGutterMark()
}