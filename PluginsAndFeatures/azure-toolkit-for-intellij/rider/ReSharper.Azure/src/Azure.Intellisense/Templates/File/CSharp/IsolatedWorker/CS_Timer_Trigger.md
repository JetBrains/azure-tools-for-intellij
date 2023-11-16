---
guid: ee9b1573-f483-4960-986e-a16242fb0607
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=TimerTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), SCHEDULE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
SCHEDULE-expression: constant("0 */5 * * * *")
---

# Timer Trigger

```
$HEADER$using System;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{    
    public class $CLASS$
    {
        private readonly ILogger _logger;

        public $CLASS$(ILoggerFactory loggerFactory)
        {
            _logger = loggerFactory.CreateLogger<$CLASS$>();
        }

        [Function("$CLASS$")]
        public void Run([TimerTrigger("$SCHEDULE$")] TimerInfo myTimer)
        {
            _logger.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");
            
            if (myTimer.ScheduleStatus is not null)
            {
                _logger.LogInformation($"Next timer schedule at: {myTimer.ScheduleStatus.Next}");$END$
            }
        }
    }
}
```