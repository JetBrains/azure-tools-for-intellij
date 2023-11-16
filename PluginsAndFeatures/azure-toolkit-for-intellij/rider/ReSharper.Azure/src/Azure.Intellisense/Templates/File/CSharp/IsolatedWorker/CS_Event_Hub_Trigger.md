---
guid: 0577eb06-8137-4417-bf62-6a7d2bc88d21
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=EventHubTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("eventHubNameValue")
CONNECTIONVALUE-expression: constant("")
---

# Event Hub Trigger

```
$HEADER$using System;
using Azure.Messaging.EventHubs;
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
        public void Run([EventHubTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")] EventData[] events)
        {
            foreach (EventData @event in events)
            {
                _logger.LogInformation("Event Body: {body}", @event.Body);
                _logger.LogInformation("Event Content-Type: {contentType}", @event.ContentType);$END$
            }
        }
    }
}
```