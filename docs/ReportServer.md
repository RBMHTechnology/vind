## 15. Report Server

**This is a work is in an alpha state**

Information about the activity of the users and how the tool performs is, for any information discovery system, of 
mayor relevance in order to improve the behaviour, provide more relevant results and track the possible errors. To 
accomplish this easily, Vind provides a `com.rbmhtechnology.vind.report.logger.ReportingSearchServer` implementation.

### 15.1 How to set it up

First step is to define your own report writer extending the class `com.rbmhtechnology.vind.report.logger.ReportWriter`, 
which behaviour is defined depending on your use of it. In this case we will use a simple ReportWriter which stores the 
entry logs in a list.

```java
    public class SimpleReportWriter extends ReportWriter {

        public ArrayList<Log> logs = new ArrayList<>();

        @Override
        public void log(Log log) {
            logs.add(log);
        }
    }
```  
To enrich the reporting logs, it is also possible to create the reporting server providing more info about the 
application which is performing the search and the current session. Then instantiate the report server based on an 
instance of any of the existing search servers implementations. 

```java
//describe your application
final Application myApp = new InterfaceApplication("myAppName", "version0.1.0",new Interface("myInterface", "version0.0.1"));

//instantiate a session object
final Session currentSession = new UserSession("sessionID", new User("userName", "userID", "user.contact@example.org"));

//Get an instance of your report writer
final ReportWriter writer = ReportWriter.getInstance();

//get an instance of the reporting server
final SearchServer reportingServer = new ReportingSearchServer(SearchServer.getInstance(), myApp, currentSession, writer);

```

**_Note:_** If no configuration is provided for the application the ReportingSearchServer will try to load from the Vind
 configuration the property _'reporting.application.id'_. If it does not exist an exception will be thrown.
 
### 15.2 How to use it
   
Once configured, using is as simple as using a basic Searchserver: define your document fields, document factory and 
execute: 

```java
    //Define field descriptor
    final SingleValueFieldDescriptor.TextFieldDescriptor<String> textField = new FieldDescriptorBuilder<String>()
            .setFacet(true)
            .buildTextField("textField");

    //Define document factory
    final DocumentFactory factory = new DocumentFactoryBuilder("asset")
            .addField(textField)
            .build();
    //execute fulltext search
    server.execute(Search.fulltext(),documentFactory);

```
All the supported actions will be added to the ReportWriter instance provided. In this case our SimpleReportWriter 
will have all the logged entries in a list. 
 
```java
    writer.logs.stream().forEach(entry -> entry.toJson());

``` 