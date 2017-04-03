# Verve Framework
Verve is a lightweight reactive framework for Java applications.
The main goals are to allow creation of server applications, replacing old Servlet API, with more scalable reactive approach, easy configuration and dependency injection inside modular code.
Verve uses Google Guice as injection engine depends and Java 8 lambdas to simplify code.


## Underneath technologies

Verve is based on several technologies, including:
- [Google Guice](https://github.com/google/guice) as IoC container, allowing to inject modules and objects
- [Eclipse Vert.x](http://vertx.io/) as a core of HTTP server implementation, but Verve introduces it's own HTTP handling interface

Verve, in comparison to Vert.x, provides safer way of coding asynchronous operations (stronger exception handling), annotations and some additional features. 


## Quickstart
Verve provides quickstart library that boots up the HTTP server and all Verve modules found using SPI (Service Provider Interface).

### How to start with Quickstart
1. Create a new project
2. Add dependency to `verve-quickstart` (for details check below)
3. Create a class extending `AbstractVerveModule` and implement required methods
4. In module fetch HttpRouter from context in set it up: 
```java
HttpRouter router = context.get(HttpRouter.class);
```

#### Main class
Run project with this class:
```
com.sproutigy.verve.VerveQuickstart
```

or create your own class with main method that calls the above one:
```
public static void main(String[] args) {
    com.sproutigy.verve.VerveQuickstart.main(args);
}
``` 

#### HTTP server port
Specify HTTP port using JVM parameter:
```
-Dverve.httpserver.port=8083
```


## Demo project
Demo project 
Contains static (e.g. `index.html`) files placed in:
```
demo/src/main/resources/com/sproutigy/verve/demo/web
```


### Demo Implementation
#### DemoModule.java
```java
@AutoService(VerveModule.class)
public class DemoModule extends AbstractVerveModule {

    @Override
    public void preInject(Context context, Binder binder) throws Exception {
    }

    @Override
    public void postInject(Context context, Injector injector) throws Exception {
        HttpRouter router = context.get(HttpRouter.class);
        router.add(injector.getInstance(Hello.class));
        router.add(injector.getInstance(Randomizer.class));
        router.add("/rpc/test", new JSONRPCHttpHandler(new TestRPC()));
        router.add(new ServeResourceHttpHandler(new ClassPathResource("web/", this.getClass())));
    }
}
```

#### Hello.java
```java
/**
* Handler that returns "Hello World" string
* 
* HttpObjectHandler may input and output any kind of class.
* It automatically serializes and deserializes input/output objects.
*/
@Singleton
@HttpRoute(path = "/hello")
public class Hello implements HttpObjectHandler<String, String> {
    @Override
    public String handle(String input, HttpRequestContext ctx) throws Exception {
        return "Hello World";
    }
}
```

#### Randomizer.java
```java
/**
 * Randomizes integer number
 * 
 * When HttpHandler returns Callable or Runnable,
 * it means that task may take long time and it should be run in worker thread
 */
@Singleton
@HttpRoute(path = "/rnd")
@HttpRoute(path = "/random")
public class Randomizer implements HttpHandler {
    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        return (Callable) () -> {
            Random rnd = new Random();
            return rnd.nextInt();
        };
    }
}
```

#### TestRPC.java
Methods of this class will be published using JSON-RPC. It's just a Plain-Old Java Object:
```java
public class TestRPC {
    public String hello() {
        val ctx = HttpRequestContext.get();
        System.out.println(ctx);
        return "world";
    }

    public long time() {
        return System.currentTimeMillis();
    }

    public String say(String text) {
        return "okay, " + text + "!";
    }
}

```
Can be called e.g. by HTTP GET:
```
/rpc/test?jsonrpc=2.0&method=hello&id=1
```

#### Custom handler
Edit `DemoModule.java` and paste following code inside `postInject` method:
```
        router.add("GET", "/here", new HttpHandler() {
            @Override
            public Object handle(HttpRequestContext ctx) throws Exception {
                System.out.println(ctx.getRequest().getMethod());
                return "DONE";
            }
        });
```

### Authentication support

Setup TokenService and AuthTokenHttpHandler in module. XSRFTokenHttpHandler should be also added for security (to prevent CSRF attacks): 
```java
    @Override
    public void preInject(Context context, Binder binder) throws Exception {
        byte[] secret = new byte[] { 65, 90, }; //TODO: fill your own secret
        JWTTokenService tokenService = new JWTTokenService("my-realm", secret);
        bind(TokenService.class).toInstance(tokenService);
    }
    
    @Override
    public void postInject(Context context, Injector injector) throws Exception {
        HttpRouter router = context.get(HttpRouter.class);
        router.add(getInstance(XSRFTokenHttpHandler.class));
        router.add(getInstance(AuthTokenHttpHandler.class));
    }
```

Sign in handler, after successful validation, token should be generated and set as cookie: 
```java
        String login = "username";
        boolean remember = false;
        String token = tokenService.generateToken(login);
        AuthTokenHttpHandler.setTokenCookie(HttpRequestContext.get(), token, "/", false, remember);
```

Anytime you can check is user authenticated and what is ID/login of the user:
```java
        boolean isAuth = AuthUtil.isAuthenticated(HttpRequestContext.get());
        String login = AuthUtil.getId(HttpRequestContext.get());
```


## Latest release
Verve is under development and still not released, but it is usable. You can treat current SNAPSHOT as Release Candidate, as there are no major API changes planned.
If you want to use **0.1-SNAPSHOT** version, you have to add OSS Sonatype Snapshots repository.

### Maven

#### Maven Repository
```xml
<repositories>
    <repository>
        <id>oss-sonatype</id>
        <name>oss-sonatype</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

#### Maven Dependency
```xml
<dependency>
    <groupId>com.sproutigy.verve</groupId>
    <artifactId>verve-quickstart</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

### Gradle

#### Gradle repository
```
repositories {
    mavenCentral()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots"
    }
}
```

#### Gradle dependency
```
dependencies {
  compile 'com.sproutigy.verve:verve-quickstart:0.1-SNAPSHOT'
}
```


## More
For more information and commercial support visit [Sproutigy](http://www.sproutigy.com/opensource)
