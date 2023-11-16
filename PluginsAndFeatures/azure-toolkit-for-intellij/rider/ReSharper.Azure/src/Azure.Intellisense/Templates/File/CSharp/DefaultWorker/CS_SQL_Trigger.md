---
guid: 4bf02cbd-c8db-46aa-ba9e-56df85f88cec
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=SqlTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
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
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Sql;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace $NAMESPACE$
{    
    public static class $CLASS$
    {
        // Visit https://aka.ms/sqltrigger to learn how to use this trigger binding
        [FunctionName("$CLASS$")]
        public static async Task RunAsync(
                [SqlTrigger("$TABLEVALUE$", "$CONNECTIONVALUE$")] IReadOnlyList<SqlChange<ToDoItem>> changes,
                ILogger log)
        {
            log.LogInformation("SQL Changes: " + JsonConvert.SerializeObject(changes));$END$
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