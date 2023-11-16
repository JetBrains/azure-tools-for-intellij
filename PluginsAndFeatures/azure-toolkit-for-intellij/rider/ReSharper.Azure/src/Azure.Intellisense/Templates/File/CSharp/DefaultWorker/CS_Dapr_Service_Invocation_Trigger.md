---
guid: 6a12542b-e634-41c1-a11b-804f08792e6e
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=DaprServiceInvocationTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
---

# Dapr Service Invocation Trigger

```
$HEADER$using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.Functions.Extensions.Dapr.Core;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Dapr;
using Microsoft.Azure.WebJobs.Extensions.Http;
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
        /// <param name="log">Function logger.</param>
        /// </summary>
        [FunctionName("$CLASS$")]
        public static async Task RunAsync(
            [DaprServiceInvocationTrigger] string payload,
            ILogger log)
        {
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
        [FunctionName("$CLASS$InvokeOutputBinding")]
        public static async Task<IActionResult> RunAsync(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "invoke/{appId}/{methodName}")] HttpRequest req,
            [DaprInvoke(AppId = "{appId}", MethodName = "{methodName}", HttpVerb = "post")] IAsyncCollector<InvokeMethodParameters> output,
            ILogger log)
        {
            log.LogInformation("C# HTTP trigger function processed a request.");

            string requestBody = await new StreamReader(req.Body).ReadToEndAsync();

            var outputContent = new InvokeMethodParameters
            {
                Body = requestBody
            };

            await output.AddAsync(outputContent);

            return new OkObjectResult("Successfully performed service invocation using Dapr invoke output binding.");
        }
    }
}
```