---
guid: b04cdc48-da71-431e-9933-e56fdd8a3022
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=CosmosDbTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), DATABASEVALUE, CONTAINERVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
DATABASEVALUE-expression: constant("databaseName")
CONTAINERVALUE-expression: constant("containerName")
CONNECTIONVALUE-expression: constant("")
---

# CosmosDB Trigger

```
$HEADER$using System;
using System.Collections.Generic;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public class $CLASS$
    {
        private readonly ILogger _logger;

        public $CLASS$(ILoggerFactory loggerFactory)
        {
            _logger = loggerFactory.CreateLogger<$CLASS$>();
        }
        
        [Function("$CLASS$")]
        public void Run([CosmosDBTrigger(
            databaseName: "$DATABASEVALUE$",
            containerName: "$CONTAINERVALUE$",
            Connection = "$CONNECTIONVALUE$",
            LeaseContainerName = "leases")] IReadOnlyList<MyDocument> input, FunctionContext context)
        {
            if (input != null && input.Count > 0)
            {
                _logger.LogInformation("Documents modified: " + input.Count);
                _logger.LogInformation("First document Id: " + input[0].Id);
            }$END$
        }
    }

    public class MyDocument
    {
        public string Id { get; set; }

        public string Text { get; set; }

        public int Number { get; set; }

        public bool Boolean { get; set; }
    }
}
```