/**
 * Copyright (c) 2018-2022 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.appservice

import com.microsoft.intellij.ui.component.AzureComponent
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JCheckBox
import javax.swing.JPanel

class AppAfterPublishSettingPanel :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")),
        AzureComponent {

    var isOpenInBrowser: Boolean
        get() = checkBoxOpenInBrowserAfterPublish.isSelected
        set(value: Boolean) { checkBoxOpenInBrowserAfterPublish.isSelected = value }

    private val checkBoxOpenInBrowserAfterPublish = JCheckBox(message("run_config.publish.form.open_in_browser_after_publish"))

    init {
        add(checkBoxOpenInBrowserAfterPublish)
    }
}
