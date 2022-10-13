---
guid: 91ebe6f2-b045-4476-87ef-d9458ec74c23
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=CosmosDbTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), DATABASEVALUE, COLLECTIONVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
DATABASEVALUE-expression: constant("databaseName")
COLLECTIONVALUE-expression: constant("collectionName")
CONNECTIONVALUE-expression: constant("")
---

# CosmosDB Trigger

```
$HEADER$namespace $NAMESPACE$

open System
open System.Collections.Generic
open Microsoft.Azure.Functions.Worker
open Microsoft.Extensions.Logging

module $CLASS$ =
    type MyDocument =
        { Id: string
          Text: string
          Number: int
          Boolean: bool }

    [<Function("$CLASS$")>]
    let run
        (
            [<CosmosDBTrigger(databaseName = "$DATABASEVALUE$",
                              collectionName = "$COLLECTIONVALUE$",
                              ConnectionStringSetting = "$CONNECTIONVALUE$",
                              LeaseCollectionName = "leases")>] input: IReadOnlyList<MyDocument>,
            context: FunctionContext
        ) =
        let logger =
            context.GetLogger "CosmsoDBTriggerFSharp"

        if not (isNull input) && input.Count > 0 then
            log.LogInformation(sprintf "Documents modified %d" input.Count)
            log.LogInformation("First document Id " + input.[0].Id)$END$
```
