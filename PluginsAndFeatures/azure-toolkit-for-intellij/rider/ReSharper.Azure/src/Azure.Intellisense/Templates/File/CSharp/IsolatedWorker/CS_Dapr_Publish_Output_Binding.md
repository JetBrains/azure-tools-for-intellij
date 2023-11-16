---
guid: 5bd6f10c-21a5-4b73-82d0-9da395333736
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=DaprPublishOutputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PUBSUBNAME, TOPICVALUE, (SCHEDULE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TOPICVALUE-expression: constant("topic")
PUBSUBNAME-expression: constant("pubsub")
SCHEDULE-expression: constant("*/10 * * * * *")
---

# Dapr Publish Output Binding

```
$HEADER$using CloudNative.CloudEvents;
using Microsoft.Azure.Functions.Extensions.Dapr.Core;
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
        /// Function will be invoked by Timer trigger and publish messages to message bus.
        /// </summary>
        /// <param name="functionContext">Function context.</param>
        [Function("$CLASS$")]
        [DaprPublishOutput(PubSubName = "$PUBSUBNAME$", Topic = "$TOPICVALUE$")]
        public static DaprPubSubEvent Run([TimerTrigger("$SCHEDULE$")] object myTimer,
                                          FunctionContext functionContext)
        {
            var log = functionContext.GetLogger("$CLASS$");
            log.LogInformation("C# DaprPublish output binding function processed a request.");

            return new DaprPubSubEvent("Invoked by Timer trigger: " + $"Hello, World! The time is {System.DateTime.UtcNow}");$END$
        }
    }

    // Below Azure function will receive message published on topic $TOPICVALUE$, and it will log the message
    public static class $CLASS$TopicTrigger
    {
        /// <summary>
        /// Visit https://aka.ms/azure-functions-dapr to learn how to use the Dapr extension.
        /// This function will get invoked when a message is published on topic $TOPICVALUE$
        /// </summary>
        /// <param name="subEvent">Cloud event sent by Dapr runtime.</param>
        /// <param name="functionContext">Function context.</param>
        [Function("$CLASS$TopicTrigger")]
        public static void Run(
            [DaprTopicTrigger("$PUBSUBNAME$", Topic = "$TOPICVALUE$")] CloudEvent subEvent,
            FunctionContext functionContext)
        {
            var log = functionContext.GetLogger("DaprTopicTriggerFuncApp");
            log.LogInformation("C# Dapr Topic Trigger function processed a request from the Dapr Runtime.");
            log.LogInformation($"Topic A received a message: {subEvent.Data}.");
        }
    }
}
```