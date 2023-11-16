---
guid: e0938da7-d4be-412b-aa79-a23745721fda
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=DaprTopicTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
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
$HEADER$using System.Text.Json;
using System.Threading.Tasks;
using CloudNative.CloudEvents;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Dapr;
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
        /// <param name="value">Value will be saved against the given key in state store.</param>
        /// <param name="log">Function logger.</param>
        [FunctionName("$CLASS$")]
        public static void Run(
            [DaprTopicTrigger("$PUBSUBNAME$", Topic = "$TOPICVALUE$")] CloudEvent subEvent,
            [DaprState("$STATESTORE$", Key = "$STATESTOREKEY$")] out object value,
            ILogger log)
        {
            log.LogInformation("C# DaprTopic trigger with DaprState output binding function processed a request from the Dapr Runtime.");

            value = subEvent.Data;$END$
        }
    }
}
```