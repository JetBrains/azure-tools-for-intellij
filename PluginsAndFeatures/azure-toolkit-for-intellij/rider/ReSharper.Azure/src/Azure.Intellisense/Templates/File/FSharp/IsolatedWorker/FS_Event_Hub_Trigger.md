---
guid: 9a3273cb-d595-4bd6-9b69-8eaf71120b55
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=EventHubTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("eventHubNameValue")
CONNECTIONVALUE-expression: constant("")
---

# Event Hub Trigger

```
$HEADER$namespace $NAMESPACE$

open System
open Azure.Messaging.EventHubs
open Microsoft.Azure.Functions.Worker
open Microsoft.Extensions.Logging
  
module $CLASS$ =
    [<Function("$CLASS$")>]
    let run
        (
            [<EventHubTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")>] events: EventData [],
            context: FunctionContext
        ) =
        let logger =
            context.GetLogger("$CLASS$")

        for event in events do
            logger.LogInformation $"Event Body: {event.Body}"
            logger.LogInformation $"Event Content-Type: {event.contentType}"$END$
```