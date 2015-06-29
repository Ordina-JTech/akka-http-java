package nl.ordina.java.akkahttp;

import akka.actor.ActorSystem;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;

import java.io.IOException;

public class Server extends HttpApp {
    @Override
    public Route createRoute() {
        return route(
                pathSingleSlash().route(
                        getFromResource("web/index.html")
                )

        );
    }

    public static void main(String[] args) throws IOException {

        ActorSystem akkaSystem = ActorSystem.create("akka-http-example");
        new Server().bindRoute("localhost", 8080, akkaSystem);

        System.out.println("<ENTER> to exit!");
        System.in.read();
        akkaSystem.shutdown();
    }
}
