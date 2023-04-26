package com.microsoft.intellij.helpers

import com.microsoft.tooling.msservices.helpers.UIHelper
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode
import java.io.File
import javax.swing.ImageIcon

class UiHelperImpl: UIHelper {
    companion object {
        fun loadIcon(name: String): ImageIcon {
            val url = UiHelperImpl::class.java.getResource("/icons/$name");
            return ImageIcon(url)
        }
    }

    override fun showException(p0: String?, p1: Throwable?, p2: String?, p3: Boolean, p4: Boolean) {
        TODO("Not yet implemented")
    }

    override fun showError(p0: String?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun showError(p0: Node?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun showConfirmation(p0: String?, p1: String?, p2: Array<out String>?, p3: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun showInfo(p0: Node?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun logError(p0: String?, p1: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun showFileChooser(p0: String?): File {
        TODO("Not yet implemented")
    }

    override fun showFileSaver(p0: String?, p1: String?): File {
        TODO("Not yet implemented")
    }

    override fun openRedisPropertyView(p0: RedisCacheNode?) {
        TODO("Not yet implemented")
    }

    override fun openRedisExplorer(p0: RedisCacheNode?) {
        TODO("Not yet implemented")
    }

    override fun openInBrowser(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun openContainerRegistryPropertyView(p0: ContainerRegistryNode?) {
        TODO("Not yet implemented")
    }

    override fun isDarkTheme(): Boolean {
        TODO("Not yet implemented")
    }
}