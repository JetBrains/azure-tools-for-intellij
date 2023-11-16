---
guid: 3c11cff7-99a9-47c5-90dd-eb39bf4adf27
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=ServiceBusQueueTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
CONNECTIONVALUE-expression: constant("")
---

# Service Bus Queue Trigger

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
        public void Run([ServiceBusTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")] ServiceBusReceivedMessage message)
        {
            _logger.LogInformation("Message ID: {id}", message.MessageId);
            _logger.LogInformation("Message Body: {body}", message.Body);
            _logger.LogInformation("Message Content-Type: {contentType}", message.ContentType);$END$
        }
    }
}
```