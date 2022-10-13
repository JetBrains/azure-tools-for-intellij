---
guid: 9e3ef753-81d7-4130-a8c9-aff5cabc23ed
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=BlobTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("samples-workitems")
CONNECTIONVALUE-expression: constant("")
---

# Blob Trigger

```
$HEADER$namespace $NAMESPACE$

open System.IO
open Microsoft.Azure.Functions.Worker
open Microsoft.Extensions.Logging

module $CLASS$ =

    [<Function("$CLASS$")>]
    let run
        (
            [<BlobTrigger("$PATHVALUE$/{name}", Connection = "$CONNECTIONVALUE$")>] myBlob: string,
            name: string,
            context: FunctionContext
        ) =
        let msg =
            sprintf "F# Blob trigger function Processed blob\nName: %s \n Data: %s" name myBlob

        let logger = context.GetLogger "BlobTriggerFSharp"
        logger.LogInformation msg$END$
```
