---
guid: 9fccc7d5-0a43-4fc2-a4d4-580d1265b536
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=QueueTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
CONNECTIONVALUE-expression: constant("")
---

# Queue Trigger

```
$HEADER$namespace $NAMESPACE$

open System
open Azure.Storage.Queues.Models
open Microsoft.Azure.Functions.Worker
open Microsoft.Extensions.Logging

module $CLASS$ =
    [<Function("$CLASS$")>]
    let run
        (
            [<QueueTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")>] message: QueueMessage,
            context: FunctionContext
        ) =
        let msg =
            sprintf "F# Queue trigger function processed: %s" message.MessageText

        let logger = context.GetLogger "$CLASS$"
        log.LogInformation msg$END$
```