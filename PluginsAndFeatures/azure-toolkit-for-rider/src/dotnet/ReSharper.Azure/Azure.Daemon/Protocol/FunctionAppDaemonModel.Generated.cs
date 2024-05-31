//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a RdGen v1.13.
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------
using System;
using System.Linq;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using JetBrains.Annotations;

using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Serialization;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Rd.Util;
using JetBrains.Rd.Text;


// ReSharper disable RedundantEmptyObjectCreationArgumentList
// ReSharper disable InconsistentNaming
// ReSharper disable RedundantOverflowCheckingContext


namespace JetBrains.Rider.Azure.Model
{
  
  
  /// <summary>
  /// <p>Generated from: FunctionAppDaemonModel.kt:14</p>
  /// </summary>
  public class FunctionAppDaemonModel : RdExtBase
  {
    //fields
    //public fields
    
    /// <summary>
    /// Signal from backend to run a Function App locally.
    /// </summary>
    [NotNull] public void RunFunctionApp(FunctionAppRequest value) => _RunFunctionApp.Fire(value);
    
    /// <summary>
    /// Signal from backend to debug a Function App locally.
    /// </summary>
    [NotNull] public void DebugFunctionApp(FunctionAppRequest value) => _DebugFunctionApp.Fire(value);
    
    /// <summary>
    /// Signal from backend to trigger a Function App.
    /// </summary>
    [NotNull] public void TriggerFunctionApp(FunctionAppTriggerRequest value) => _TriggerFunctionApp.Fire(value);
    
    /// <summary>
    /// Request from frontend to read the AzureFunctionsVersion MSBuild property.
    /// </summary>
    [NotNull] public IRdEndpoint<AzureFunctionsVersionRequest, string> GetAzureFunctionsVersion => _GetAzureFunctionsVersion;
    
    //private fields
    [NotNull] private readonly RdSignal<FunctionAppRequest> _RunFunctionApp;
    [NotNull] private readonly RdSignal<FunctionAppRequest> _DebugFunctionApp;
    [NotNull] private readonly RdSignal<FunctionAppTriggerRequest> _TriggerFunctionApp;
    [NotNull] private readonly RdCall<AzureFunctionsVersionRequest, string> _GetAzureFunctionsVersion;
    
    //primary constructor
    private FunctionAppDaemonModel(
      [NotNull] RdSignal<FunctionAppRequest> runFunctionApp,
      [NotNull] RdSignal<FunctionAppRequest> debugFunctionApp,
      [NotNull] RdSignal<FunctionAppTriggerRequest> triggerFunctionApp,
      [NotNull] RdCall<AzureFunctionsVersionRequest, string> getAzureFunctionsVersion
    )
    {
      if (runFunctionApp == null) throw new ArgumentNullException("runFunctionApp");
      if (debugFunctionApp == null) throw new ArgumentNullException("debugFunctionApp");
      if (triggerFunctionApp == null) throw new ArgumentNullException("triggerFunctionApp");
      if (getAzureFunctionsVersion == null) throw new ArgumentNullException("getAzureFunctionsVersion");
      
      _RunFunctionApp = runFunctionApp;
      _DebugFunctionApp = debugFunctionApp;
      _TriggerFunctionApp = triggerFunctionApp;
      _GetAzureFunctionsVersion = getAzureFunctionsVersion;
      _GetAzureFunctionsVersion.ValueCanBeNull = true;
      BindableChildren.Add(new KeyValuePair<string, object>("runFunctionApp", _RunFunctionApp));
      BindableChildren.Add(new KeyValuePair<string, object>("debugFunctionApp", _DebugFunctionApp));
      BindableChildren.Add(new KeyValuePair<string, object>("triggerFunctionApp", _TriggerFunctionApp));
      BindableChildren.Add(new KeyValuePair<string, object>("getAzureFunctionsVersion", _GetAzureFunctionsVersion));
    }
    //secondary constructor
    internal FunctionAppDaemonModel (
    ) : this (
      new RdSignal<FunctionAppRequest>(FunctionAppRequest.Read, FunctionAppRequest.Write),
      new RdSignal<FunctionAppRequest>(FunctionAppRequest.Read, FunctionAppRequest.Write),
      new RdSignal<FunctionAppTriggerRequest>(FunctionAppTriggerRequest.Read, FunctionAppTriggerRequest.Write),
      new RdCall<AzureFunctionsVersionRequest, string>(AzureFunctionsVersionRequest.Read, AzureFunctionsVersionRequest.Write, ReadStringNullable, WriteStringNullable)
    ) {}
    //deconstruct trait
    //statics
    
    public static CtxReadDelegate<string> ReadStringNullable = JetBrains.Rd.Impl.Serializers.ReadString.NullableClass();
    
    public static  CtxWriteDelegate<string> WriteStringNullable = JetBrains.Rd.Impl.Serializers.WriteString.NullableClass();
    
    protected override long SerializationHash => 8461808307497795513L;
    
    protected override Action<ISerializers> Register => RegisterDeclaredTypesSerializers;
    public static void RegisterDeclaredTypesSerializers(ISerializers serializers)
    {
      
      serializers.RegisterToplevelOnce(typeof(JetBrains.Rider.Model.IdeRoot), JetBrains.Rider.Model.IdeRoot.RegisterDeclaredTypesSerializers);
    }
    
    
    //constants
    
    //custom body
    //methods
    //equals trait
    //hash code trait
    //pretty print
    public override void Print(PrettyPrinter printer)
    {
      printer.Println("FunctionAppDaemonModel (");
      using (printer.IndentCookie()) {
        printer.Print("runFunctionApp = "); _RunFunctionApp.PrintEx(printer); printer.Println();
        printer.Print("debugFunctionApp = "); _DebugFunctionApp.PrintEx(printer); printer.Println();
        printer.Print("triggerFunctionApp = "); _TriggerFunctionApp.PrintEx(printer); printer.Println();
        printer.Print("getAzureFunctionsVersion = "); _GetAzureFunctionsVersion.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
  public static class SolutionFunctionAppDaemonModelEx
   {
    public static FunctionAppDaemonModel GetFunctionAppDaemonModel(this JetBrains.Rider.Model.Solution solution)
    {
      return solution.GetOrCreateExtension("functionAppDaemonModel", () => new FunctionAppDaemonModel());
    }
  }
  
  
  /// <summary>
  /// <p>Generated from: FunctionAppDaemonModel.kt:41</p>
  /// </summary>
  public sealed class AzureFunctionsVersionRequest : IPrintable, IEquatable<AzureFunctionsVersionRequest>
  {
    //fields
    //public fields
    [NotNull] public string ProjectFilePath {get; private set;}
    
    //private fields
    //primary constructor
    public AzureFunctionsVersionRequest(
      [NotNull] string projectFilePath
    )
    {
      if (projectFilePath == null) throw new ArgumentNullException("projectFilePath");
      
      ProjectFilePath = projectFilePath;
    }
    //secondary constructor
    //deconstruct trait
    public void Deconstruct([NotNull] out string projectFilePath)
    {
      projectFilePath = ProjectFilePath;
    }
    //statics
    
    public static CtxReadDelegate<AzureFunctionsVersionRequest> Read = (ctx, reader) => 
    {
      var projectFilePath = reader.ReadString();
      var _result = new AzureFunctionsVersionRequest(projectFilePath);
      return _result;
    };
    
    public static CtxWriteDelegate<AzureFunctionsVersionRequest> Write = (ctx, writer, value) => 
    {
      writer.Write(value.ProjectFilePath);
    };
    
    //constants
    
    //custom body
    //methods
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((AzureFunctionsVersionRequest) obj);
    }
    public bool Equals(AzureFunctionsVersionRequest other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return ProjectFilePath == other.ProjectFilePath;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + ProjectFilePath.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("AzureFunctionsVersionRequest (");
      using (printer.IndentCookie()) {
        printer.Print("projectFilePath = "); ProjectFilePath.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
  
  
  /// <summary>
  /// <p>Generated from: FunctionAppDaemonModel.kt:20</p>
  /// </summary>
  public sealed class FunctionAppHttpTriggerAttribute : IPrintable, IEquatable<FunctionAppHttpTriggerAttribute>
  {
    //fields
    //public fields
    [CanBeNull] public string AuthLevel {get; private set;}
    [NotNull] public List<string> Methods {get; private set;}
    [CanBeNull] public string Route {get; private set;}
    [CanBeNull] public string RouteForHttpClient {get; private set;}
    
    //private fields
    //primary constructor
    public FunctionAppHttpTriggerAttribute(
      [CanBeNull] string authLevel,
      [NotNull] List<string> methods,
      [CanBeNull] string route,
      [CanBeNull] string routeForHttpClient
    )
    {
      if (methods == null) throw new ArgumentNullException("methods");
      
      AuthLevel = authLevel;
      Methods = methods;
      Route = route;
      RouteForHttpClient = routeForHttpClient;
    }
    //secondary constructor
    //deconstruct trait
    public void Deconstruct([CanBeNull] out string authLevel, [NotNull] out List<string> methods, [CanBeNull] out string route, [CanBeNull] out string routeForHttpClient)
    {
      authLevel = AuthLevel;
      methods = Methods;
      route = Route;
      routeForHttpClient = RouteForHttpClient;
    }
    //statics
    
    public static CtxReadDelegate<FunctionAppHttpTriggerAttribute> Read = (ctx, reader) => 
    {
      var authLevel = ReadStringNullable(ctx, reader);
      var methods = ReadStringNullableList(ctx, reader);
      var route = ReadStringNullable(ctx, reader);
      var routeForHttpClient = ReadStringNullable(ctx, reader);
      var _result = new FunctionAppHttpTriggerAttribute(authLevel, methods, route, routeForHttpClient);
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringNullable = JetBrains.Rd.Impl.Serializers.ReadString.NullableClass();
    public static CtxReadDelegate<List<string>> ReadStringNullableList = JetBrains.Rd.Impl.Serializers.ReadString.NullableClass().List();
    
    public static CtxWriteDelegate<FunctionAppHttpTriggerAttribute> Write = (ctx, writer, value) => 
    {
      WriteStringNullable(ctx, writer, value.AuthLevel);
      WriteStringNullableList(ctx, writer, value.Methods);
      WriteStringNullable(ctx, writer, value.Route);
      WriteStringNullable(ctx, writer, value.RouteForHttpClient);
    };
    public static  CtxWriteDelegate<string> WriteStringNullable = JetBrains.Rd.Impl.Serializers.WriteString.NullableClass();
    public static  CtxWriteDelegate<List<string>> WriteStringNullableList = JetBrains.Rd.Impl.Serializers.WriteString.NullableClass().List();
    
    //constants
    
    //custom body
    //methods
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((FunctionAppHttpTriggerAttribute) obj);
    }
    public bool Equals(FunctionAppHttpTriggerAttribute other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Equals(AuthLevel, other.AuthLevel) && Methods.SequenceEqual(other.Methods) && Equals(Route, other.Route) && Equals(RouteForHttpClient, other.RouteForHttpClient);
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + (AuthLevel != null ? AuthLevel.GetHashCode() : 0);
        hash = hash * 31 + Methods.ContentHashCode();
        hash = hash * 31 + (Route != null ? Route.GetHashCode() : 0);
        hash = hash * 31 + (RouteForHttpClient != null ? RouteForHttpClient.GetHashCode() : 0);
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("FunctionAppHttpTriggerAttribute (");
      using (printer.IndentCookie()) {
        printer.Print("authLevel = "); AuthLevel.PrintEx(printer); printer.Println();
        printer.Print("methods = "); Methods.PrintEx(printer); printer.Println();
        printer.Print("route = "); Route.PrintEx(printer); printer.Println();
        printer.Print("routeForHttpClient = "); RouteForHttpClient.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
  
  
  /// <summary>
  /// <p>Generated from: FunctionAppDaemonModel.kt:35</p>
  /// </summary>
  public sealed class FunctionAppRequest : IPrintable, IEquatable<FunctionAppRequest>
  {
    //fields
    //public fields
    [CanBeNull] public string MethodName {get; private set;}
    [CanBeNull] public string FunctionName {get; private set;}
    [NotNull] public string ProjectFilePath {get; private set;}
    
    //private fields
    //primary constructor
    public FunctionAppRequest(
      [CanBeNull] string methodName,
      [CanBeNull] string functionName,
      [NotNull] string projectFilePath
    )
    {
      if (projectFilePath == null) throw new ArgumentNullException("projectFilePath");
      
      MethodName = methodName;
      FunctionName = functionName;
      ProjectFilePath = projectFilePath;
    }
    //secondary constructor
    //deconstruct trait
    public void Deconstruct([CanBeNull] out string methodName, [CanBeNull] out string functionName, [NotNull] out string projectFilePath)
    {
      methodName = MethodName;
      functionName = FunctionName;
      projectFilePath = ProjectFilePath;
    }
    //statics
    
    public static CtxReadDelegate<FunctionAppRequest> Read = (ctx, reader) => 
    {
      var methodName = ReadStringNullable(ctx, reader);
      var functionName = ReadStringNullable(ctx, reader);
      var projectFilePath = reader.ReadString();
      var _result = new FunctionAppRequest(methodName, functionName, projectFilePath);
      return _result;
    };
    public static CtxReadDelegate<string> ReadStringNullable = JetBrains.Rd.Impl.Serializers.ReadString.NullableClass();
    
    public static CtxWriteDelegate<FunctionAppRequest> Write = (ctx, writer, value) => 
    {
      WriteStringNullable(ctx, writer, value.MethodName);
      WriteStringNullable(ctx, writer, value.FunctionName);
      writer.Write(value.ProjectFilePath);
    };
    public static  CtxWriteDelegate<string> WriteStringNullable = JetBrains.Rd.Impl.Serializers.WriteString.NullableClass();
    
    //constants
    
    //custom body
    //methods
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((FunctionAppRequest) obj);
    }
    public bool Equals(FunctionAppRequest other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Equals(MethodName, other.MethodName) && Equals(FunctionName, other.FunctionName) && ProjectFilePath == other.ProjectFilePath;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + (MethodName != null ? MethodName.GetHashCode() : 0);
        hash = hash * 31 + (FunctionName != null ? FunctionName.GetHashCode() : 0);
        hash = hash * 31 + ProjectFilePath.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("FunctionAppRequest (");
      using (printer.IndentCookie()) {
        printer.Print("methodName = "); MethodName.PrintEx(printer); printer.Println();
        printer.Print("functionName = "); FunctionName.PrintEx(printer); printer.Println();
        printer.Print("projectFilePath = "); ProjectFilePath.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
  
  
  /// <summary>
  /// <p>Generated from: FunctionAppDaemonModel.kt:27</p>
  /// </summary>
  public sealed class FunctionAppTriggerRequest : IPrintable, IEquatable<FunctionAppTriggerRequest>
  {
    //fields
    //public fields
    [NotNull] public string MethodName {get; private set;}
    [NotNull] public string FunctionName {get; private set;}
    public FunctionAppTriggerType TriggerType {get; private set;}
    [CanBeNull] public FunctionAppHttpTriggerAttribute HttpTriggerAttribute {get; private set;}
    [NotNull] public string ProjectFilePath {get; private set;}
    
    //private fields
    //primary constructor
    public FunctionAppTriggerRequest(
      [NotNull] string methodName,
      [NotNull] string functionName,
      FunctionAppTriggerType triggerType,
      [CanBeNull] FunctionAppHttpTriggerAttribute httpTriggerAttribute,
      [NotNull] string projectFilePath
    )
    {
      if (methodName == null) throw new ArgumentNullException("methodName");
      if (functionName == null) throw new ArgumentNullException("functionName");
      if (projectFilePath == null) throw new ArgumentNullException("projectFilePath");
      
      MethodName = methodName;
      FunctionName = functionName;
      TriggerType = triggerType;
      HttpTriggerAttribute = httpTriggerAttribute;
      ProjectFilePath = projectFilePath;
    }
    //secondary constructor
    //deconstruct trait
    public void Deconstruct([NotNull] out string methodName, [NotNull] out string functionName, out FunctionAppTriggerType triggerType, [CanBeNull] out FunctionAppHttpTriggerAttribute httpTriggerAttribute, [NotNull] out string projectFilePath)
    {
      methodName = MethodName;
      functionName = FunctionName;
      triggerType = TriggerType;
      httpTriggerAttribute = HttpTriggerAttribute;
      projectFilePath = ProjectFilePath;
    }
    //statics
    
    public static CtxReadDelegate<FunctionAppTriggerRequest> Read = (ctx, reader) => 
    {
      var methodName = reader.ReadString();
      var functionName = reader.ReadString();
      var triggerType = (FunctionAppTriggerType)reader.ReadInt();
      var httpTriggerAttribute = ReadFunctionAppHttpTriggerAttributeNullable(ctx, reader);
      var projectFilePath = reader.ReadString();
      var _result = new FunctionAppTriggerRequest(methodName, functionName, triggerType, httpTriggerAttribute, projectFilePath);
      return _result;
    };
    public static CtxReadDelegate<FunctionAppHttpTriggerAttribute> ReadFunctionAppHttpTriggerAttributeNullable = FunctionAppHttpTriggerAttribute.Read.NullableClass();
    
    public static CtxWriteDelegate<FunctionAppTriggerRequest> Write = (ctx, writer, value) => 
    {
      writer.Write(value.MethodName);
      writer.Write(value.FunctionName);
      writer.Write((int)value.TriggerType);
      WriteFunctionAppHttpTriggerAttributeNullable(ctx, writer, value.HttpTriggerAttribute);
      writer.Write(value.ProjectFilePath);
    };
    public static  CtxWriteDelegate<FunctionAppHttpTriggerAttribute> WriteFunctionAppHttpTriggerAttributeNullable = FunctionAppHttpTriggerAttribute.Write.NullableClass();
    
    //constants
    
    //custom body
    //methods
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((FunctionAppTriggerRequest) obj);
    }
    public bool Equals(FunctionAppTriggerRequest other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return MethodName == other.MethodName && FunctionName == other.FunctionName && TriggerType == other.TriggerType && Equals(HttpTriggerAttribute, other.HttpTriggerAttribute) && ProjectFilePath == other.ProjectFilePath;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + MethodName.GetHashCode();
        hash = hash * 31 + FunctionName.GetHashCode();
        hash = hash * 31 + (int) TriggerType;
        hash = hash * 31 + (HttpTriggerAttribute != null ? HttpTriggerAttribute.GetHashCode() : 0);
        hash = hash * 31 + ProjectFilePath.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("FunctionAppTriggerRequest (");
      using (printer.IndentCookie()) {
        printer.Print("methodName = "); MethodName.PrintEx(printer); printer.Println();
        printer.Print("functionName = "); FunctionName.PrintEx(printer); printer.Println();
        printer.Print("triggerType = "); TriggerType.PrintEx(printer); printer.Println();
        printer.Print("httpTriggerAttribute = "); HttpTriggerAttribute.PrintEx(printer); printer.Println();
        printer.Print("projectFilePath = "); ProjectFilePath.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
  
  
  /// <summary>
  /// <p>Generated from: FunctionAppDaemonModel.kt:15</p>
  /// </summary>
  public enum FunctionAppTriggerType {
    HttpTrigger,
    Other
  }
}