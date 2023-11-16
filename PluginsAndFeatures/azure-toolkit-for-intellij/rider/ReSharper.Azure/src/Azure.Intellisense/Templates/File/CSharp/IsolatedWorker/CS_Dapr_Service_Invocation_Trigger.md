---
guid: 5c9abf80-0f16-48dd-bc66-d6b3c9fcf7d4
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=DaprServiceInvocationTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
---

# Dapr Service Invocation Trigger

```
$HEADER$using Microsoft.Azure.Functions.Extensions.Dapr.Core;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Extensions.Dapr;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Extensions.Logging;
using System.IO;
using System.Threading.Tasks;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        /// <summary>
        /// Visit https://aka.ms/azure-functions-dapr to learn how to use the Dapr extension.
        /// These tasks should be completed prior to running :
        ///   1. Install Dapr
        /// Start function app with Dapr: dapr run --app-id functionapp --app-port 3001 --dapr-http-port 3501 -- func host start
        /// Invoke function app by dapr cli: dapr invoke --app-id functionapp --method {yourFunctionName}  --data '{ \"data\": {\"value\": { \"orderId\": \"41\" } } }'
        /// Invoke function app by http trigger: 
        /// curl 'http://localhost:7071/api/invoke/functionapp/{yourFunctionName}' `
        /// --header 'Content-Type: application/json' `
        /// --data '{
        ///     "data": {
        ///         "value": {
        ///             "orderId": "41"
        ///         }
        ///     }
        /// }'
        /// <param name="payload">Payload of dapr service invocation trigger.</param>
        /// <param name="functionContext">Function context.</param>
        /// </summary>
        [Function("$CLASS$")]
        public static void Run(
            [DaprServiceInvocationTrigger] string payload,
            FunctionContext functionContext)
        {
            var log = functionContext.GetLogger("$CLASS$");
            log.LogInformation("Azure function triggered by Dapr Service Invocation Trigger.");
            log.LogInformation($"Dapr service invocation trigger payload: {payload}");$END$
        }
    }

    public static class $CLASS$InvokeOutputBinding
    {
        /// <summary>
        /// Sample to use a Dapr Invoke Output Binding to perform a Dapr Server Invocation operation hosted in another Darp'd app.
        /// Here this function acts like a proxy
        /// </summary>
        [Function("$CLASS$InvokeOutputBinding")]
        [DaprInvokeOutput(AppId = "{appId}", MethodName = "{methodName}", HttpVerb = "post")]
        public static async Task<InvokeMethodParameters> Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "invoke/{appId}/{methodName}")] HttpRequestData req, FunctionContext functionContext)
        {
            var log = functionContext.GetLogger("InvokeOutputBinding");
            log.LogInformation("C# HTTP trigger function processed a request.");

            string requestBody = await new StreamReader(req.Body).ReadToEndAsync();

            var outputContent = new InvokeMethodParameters
            {
                Body = requestBody
            };

            return outputContent;
        }
    }
}
```