---
guid: 98104b0a-97de-4847-b8f0-5b9f438bdc92
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=HttpTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,Admin")
---

# HTTP Trigger

```
$HEADER$namespace $NAMESPACE$

open System.Net
open Microsoft.Azure.Functions.Worker
open Microsoft.Azure.Functions.Worker.Http
open Microsoft.Extensions.Logging

module $CLASS$ =

    [<Function("$CLASS$")>]
    let run
        ([<HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "get", "post", Route = null)>] req: HttpRequestData)
        (context: FunctionContext)
        =
        let logger = context.GetLogger "$CLASS$"
        logger.LogInformation "F# HTTP trigger function processed a request"

        let response = req.CreateResponse(HttpStatusCode.OK)
        response.Headers.Add("Content-Type", "text/plain; charset=utf-8")

        response.WriteString "Welcome to Azure Functions!"

        response$END$
```