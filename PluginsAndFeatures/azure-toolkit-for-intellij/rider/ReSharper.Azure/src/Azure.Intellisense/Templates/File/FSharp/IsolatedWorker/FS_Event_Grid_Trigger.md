---
guid: 9c32fa2b-ec21-4789-ba43-b5a897fb8f5b
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=EventGridTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
---

# Event Grid Trigger

```
$HEADER$namespace $NAMESPACE$
// Default URL for triggering event grid function in the local environment.
// http://localhost:7071/runtime/webhooks/EventGrid?functionName={functionname}

open System
open Microsoft.Azure.Functions.Worker
open Microsoft.Extensions.Logging

module $CLASS$ =
    type MyEvent =
        { Id: string
          Topic: string
          Subject: string
          EventType: string
          EventTime: DateTime
          Data: object }

    [<Function("$CLASS$")>]
    let run ([<EventGridTrigger>] eventGridEvent: MyEvent, context: FunctionContext) =
        let logger =
            context.GetLogger("EventGridTriggerFSharp")

        log.LogInformation(eventGridEvent.Data.ToString())$END$
```
