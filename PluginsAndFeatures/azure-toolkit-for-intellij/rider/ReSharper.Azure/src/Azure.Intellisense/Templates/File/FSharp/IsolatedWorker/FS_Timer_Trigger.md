---
guid: 98a9a23c-c542-4e82-af8d-4a1bb410a6b2
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=TimerTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), SCHEDULE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
SCHEDULE-expression: constant("0 */5 * * * *")
---

# Timer Trigger

```
$HEADER$namespace $NAMESPACE$

open System
open Microsoft.Azure.Functions.Worker
open Microsoft.Extensions.Logging

module $CLASS$ =
    [<Function("$CLASS$")>]
    let run ([<TimerTrigger("$SCHEDULE$")>] myTimer: TimerInfo, context: FunctionContext) =
        let logger = context.GetLogger "$CLASS$"
        logger.LogInformation(sprintf "F# Time trigger function executed at: %A" DateTime.Now)
        logger.LogInformation(sprintf "Next timer schedule at: %A" myTimer.ScheduleStatus.Next)$END$
```