---
guid: 7f50ad96-6a80-4be0-96b8-9d224997a9aa
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=ServiceBusTopicTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), TOPICNAME, SUBSCRIPTIONNAME, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TOPICNAME-expression: constant("mytopic")
SUBSCRIPTIONNAME-expression: constant("mysubscription")
CONNECTIONVALUE-expression: constant("")
---

# Service Bus Topic Trigger

```
$HEADER$using System;
using Azure.Messaging.ServiceBus;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public class $CLASS$
    {
        private readonly ILogger<$CLASS$> _logger;

        public $CLASS$(ILogger<$CLASS$> logger)
        {
            _logger = logger;
        }

        [Function(nameof($CLASS$))]
        public void Run([ServiceBusTrigger("$TOPICNAME$", "$SUBSCRIPTIONNAME$", Connection = "$CONNECTIONVALUE$")] ServiceBusReceivedMessage message)
        {
            _logger.LogInformation("Message ID: {id}", message.MessageId);
            _logger.LogInformation("Message Body: {body}", message.Body);
            _logger.LogInformation("Message Content-Type: {contentType}", message.ContentType);$END$
        }
    }
}

```