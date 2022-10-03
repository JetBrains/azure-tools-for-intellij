## Azure Toolkit for Rider
### Prerequisites
- Install [Corretto JDK 8](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
- Install [Corretto JDK 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)

#### Windows
- Make sure to install [Visual Studio 2010 C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist?
  view=msvc-170#visual-studio-2010-vc-100-sp1-no-longer-supported).

  Otherwise, you'll get errors related to exit code -1073741515 during the Utils project installation.

### Building
* Clone and Build [azure-maven-plugins/azure-toolkit-libs](https://github.com/microsoft/azure-maven-plugins/tree/develop/azure-toolkit-libs) first. 
  Azure Toolkit for Intellij doesn't depend on our maven plugins, but they share some code together.
    ```
    $ git clone https://github.com/microsoft/azure-maven-plugins.git
    $ cd azure-maven-plugins/azure-toolkit-libs
    $ mvn clean install
    ```
* Clone the repository with HTTPS or SSH:
    ```
    $ git clone https://github.com/JetBrains/azure-tools-for-intellij.git
    $ cd azure-tools-for-intellij
    ```
* **Using JDK 8**, run the following command under the project base path:
    ```
    $ ./mvnw clean install -f Utils/pom.xml
    ```

    (If you have problems, make sure `JAVA_HOME` environment variable points to `<JDK8>/bin`.)
* **Using JDK 17**, use Gradle to build the plugin
    ```
    $ ./gradlew -b PluginsAndFeatures/azure-toolkit-for-intellij/build.gradle buildPlugin
    ```
    You can find the outputs under ```PluginsAndFeatures/azure-toolkit-for-intellij/build/distributions```
    
### Run/Debug
* Open IntelliJ, open PluginsAndFeatures/azure-toolkit-for-intellij.
* Run/Debug the plugin by triggering the Gradle task as following:
    ![intellij_run_debug](docs/resources/intellij_run_debug.png)

## Azure Toolkit for Eclipse
### Building
* Clone the repository with HTTPS or SSH:
    ```
    $ git clone https://github.com/Microsoft/azure-tools-for-java.git
    $ cd azure-tools-for-java
    ```
* Run the following command under the project base path:
    ```
    $ mvn clean install -f Utils/pom.xml
    $ mvn clean install -f PluginsAndFeatures/AddLibrary/AzureLibraries/pom.xml
    ```
* Use Maven to build the plugin
    ```
    mvn clean install -f PluginsAndFeatures/azure-toolkit-for-eclipse/pom.xml
    ```
    You can find the outputs under ```PluginsAndFeatures/azure-toolkit-for-eclipse/WindowsAzurePlugin4EJ/target```

### Run/Debug
* Open Eclipse, select ```import > Maven > Existing Maven Projects```:
    ![eclipse_import_step1](docs/resources/eclipse_import_step1.png)
* Import all the modules under ```PluginsAndFeatures/azure-toolkit-for-eclipse```:
    ![eclipse_import_step2](docs/resources/eclipse_import_step2.png)
* New a run/debug configuration and click Run/Debug:
    ![eclipse_debug](docs/resources/eclipse_debug.png)
