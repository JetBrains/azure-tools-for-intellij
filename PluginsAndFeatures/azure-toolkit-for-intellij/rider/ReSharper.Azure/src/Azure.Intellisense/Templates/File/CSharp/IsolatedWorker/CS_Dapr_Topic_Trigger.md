---
guid: d9d5ba30-b25c-4010-bf55-94fa43e880f4
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=DaprTopicTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PUBSUBNAME, TOPICVALUE, STATESTORE, STATESTOREKEY
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TOPICVALUE-expression: constant("topic")
PUBSUBNAME-expression: constant("pubsub")
STATESTORE-expression: constant("statestore")
STATESTOREKEY-expression: constant("product")
---

# Dapr Topic Trigger

```
$HEADER$using System.Text;
using CloudNative.CloudEvents;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Extensions.Dapr;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        /// <summary>
        /// Visit https://aka.ms/azure-functions-dapr to learn how to use the Dapr extension.
        /// These tasks should be completed prior to running :
        ///   1. Install Dapr
        /// Start function app with Dapr: dapr run --app-id functionapp --app-port 3001 --dapr-http-port 3501 -- func host start
        /// Invoke function app: dapr publish --pubsub pubsub --publish-app-id functionapp --topic $TOPICVALUE$ --data '{\"value\": { \"orderId\": \"42\" } }'
        /// </summary>
        /// <param name="subEvent">Cloud event sent by Dapr runtime.</param>
        /// <param name="functionContext">Function context.</param>
        [Function("$CLASS$")]
        [DaprStateOutput("$STATESTORE$", Key = "$STATESTOREKEY$")]
        public static object? Run(
            [DaprTopicTrigger("$PUBSUBNAME$", Topic = "$TOPICVALUE$")] CloudEvent subEvent,
            FunctionContext functionContext)
        {
            var log = functionContext.GetLogger("$CLASS$");
            log.LogInformation("C# DaprTopic trigger with DaprState output binding function processed a request.");
    
            return subEvent.Data;
        }
    }
}
```