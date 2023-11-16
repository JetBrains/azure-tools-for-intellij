---
guid: f1ebe6f2-b045-4476-87ef-d9458ec74c23
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=CosmosDbTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsDefaultWorker
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
$HEADER$namespace $NAMESPACE$

open System.Collections.Generic
open Microsoft.Azure.Documents
open Microsoft.Azure.WebJobs
open Microsoft.Azure.WebJobs.Host
open Microsoft.Extensions.Logging

module $CLASS$ =
    type TodoItem =
        { id: string
          Description: string }

    [<FunctionName("$CLASS$")>]
    let run([<CosmosDBTrigger(databaseName="$DATABASEVALUE$", containerName="$CONTAINERVALUE$", Connection="$CONNECTIONVALUE$", LeaseContainerName="leases")>] input: IReadOnlyList<TodoItem>, log: ILogger) =
        if not(isNull input) && input.Count > 0 then
            log.LogInformation(sprintf "Documents modified %d" input.Count)
            log.LogInformation("First document Id " + input.[0].id)$END$
```