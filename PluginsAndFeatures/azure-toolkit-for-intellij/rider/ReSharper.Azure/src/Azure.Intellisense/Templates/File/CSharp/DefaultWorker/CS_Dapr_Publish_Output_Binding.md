---
guid: a50d2862-4c19-4ab2-90d8-4b61be652e5f
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=DaprPublishOutputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
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
$HEADER$using System.Threading.Tasks;
using CloudNative.CloudEvents;
using Microsoft.Azure.Functions.Extensions.Dapr.Core;
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
        /// Function will be invoked by Timer trigger and publish messages to message bus.
        /// </summary>
        /// <param name="log">Function logger.</param>
        [FunctionName("$CLASS$")]
        //  we can try using Function timer trigger.
        // Rename the template to PublishTemplate
        public static void Run([TimerTrigger("$SCHEDULE$")] TimerInfo myTimer,
                               [DaprPublish(PubSubName = "$PUBSUBNAME$", Topic = "$TOPICVALUE$")] out DaprPubSubEvent pubEvent,
                               ILogger log)
        {
            log.LogInformation("C# DaprPublish output binding function processed a request.");

            pubEvent = new DaprPubSubEvent("Invoked by Timer trigger: " + $"Hello, World! The time is {System.DateTime.UtcNow}");$END$
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
        /// <param name="log">Function logger.</param>
        [FunctionName("$CLASS$TopicTrigger")]
        public static async Task RunAsync(
            [DaprTopicTrigger("$PUBSUBNAME$", Topic = "$TOPICVALUE$")] CloudEvent subEvent,
            ILogger log)
        {
            log.LogInformation("C# Dapr Topic Trigger function processed a request from the Dapr Runtime.");
            log.LogInformation($"Topic A received a message: {subEvent.Data}.");
        }
    }
}
```