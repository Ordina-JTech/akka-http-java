package nl.ordina.java.akkahttp;

import akka.actor.ActorSystem;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;
import com.google.inject.Guice;
import com.google.inject.Injector;

import javax.inject.Inject;
import java.io.IOException;

import static akka.http.javadsl.model.HttpResponse.create;
import static akka.http.scaladsl.model.StatusCodes.InternalServerError;

public class Server extends HttpApp {


  private final GroupHttp groupHttp;

  @Inject
  public Server(GroupHttp groupHttp) {
    this.groupHttp = groupHttp;
  }

  @Override
  public Route createRoute() {
    return handleExceptions(e -> {
          e.printStackTrace();
          return complete(create().withStatus(InternalServerError()));
        },
        pathSingleSlash().route(
            getFromResource("web/index.html")
        ),
        groupHttp.groups()
    );
  }


  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    Server server = injector.getInstance(Server.class);

    ActorSystem akkaSystem = ActorSystem.create("akka-http-example");
    server.bindRoute("localhost", 8080, akkaSystem);

    System.out.println("<ENTER> to exit!");
    System.in.read();
    akkaSystem.shutdown();
  }
}
