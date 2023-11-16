---
guid: 707c458f-6b00-4ae6-aba8-0a02606c76be
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=SqlTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), TABLEVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TABLEVALUE-expression: constant("[dbo].[table1]")
CONNECTIONVALUE-expression: constant("")
---

# SQL Trigger

```
$HEADER$using System;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Azure.Functions.Worker.Extensions.Sql;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace $NAMESPACE$
{
    public class $CLASS$
    {
        private readonly ILogger _logger;

        public $CLASS$(ILoggerFactory loggerFactory)
        {
            _logger = loggerFactory.CreateLogger<$CLASS$>();
        }

        // Visit https://aka.ms/sqltrigger to learn how to use this trigger binding
        [Function("$CLASS$")]
        public void Run(
            [SqlTrigger("$TABLEVALUE$", "$CONNECTIONVALUE$")] IReadOnlyList<SqlChange<ToDoItem>> changes,
                FunctionContext context)
        {
            _logger.LogInformation("SQL Changes: " + JsonConvert.SerializeObject(changes));$END$
        }
    }

    public class ToDoItem
    {
        public string Id { get; set; }
        public int Priority { get; set; }
        public string Description { get; set; }
    }
}
```