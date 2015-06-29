package nl.ordina.java.akkahttp;

import akka.actor.ActorSystem;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.values.PathMatcher;
import akka.http.scaladsl.model.StatusCodes;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static akka.http.javadsl.marshallers.jackson.Jackson.jsonAs;
import static akka.http.javadsl.model.HttpResponse.create;
import static akka.http.javadsl.server.RequestVals.entityAs;
import static akka.http.javadsl.server.values.PathMatchers.uuid;
import static akka.http.scaladsl.model.StatusCodes.Created;
import static akka.http.scaladsl.model.StatusCodes.InternalServerError;

public class Server extends HttpApp {
    private GroupRepo groups = new GroupRepo();

    @Override
    public Route createRoute() {

        /* Dezelfde instantie moet gebruikt worden voor het opvragen van een PathMatcher! */
        PathMatcher<UUID> uuidExtractor = uuid();

        return handleExceptions(e -> {
                    e.printStackTrace();
                    return complete(create().withStatus(InternalServerError()));
                },
                pathSingleSlash().route(
                        getFromResource("web/index.html")
                ),
                pathPrefix("groups").route(
                        get(pathEndOrSingleSlash().route(
                                handleWith(ctx -> ctx.completeAs(Jackson.json(), groups.getAll()))
                        )),
                        get(path(uuidExtractor).route(
                                handleWith(uuidExtractor,
                                        (ctx, uuid) -> ctx.completeAs(Jackson.json(), groups.get(uuid))
                                )
                        )),
                        post(
                                handleWith(entityAs(jsonAs(Group.class)),
                                        (ctx, group) -> {
                                            Group saved = groups.create(group);
                                            return ctx.complete(HttpResponse.create()
                                                    .withStatus(Created())
                                                    .addHeader(
                                                            Location.create(
                                                                    Uri.create("http://localhost:8080/groups/" + saved.getUuid()))));
                                        })
                        ),
                        put(path(uuidExtractor).route(
                                handleWith(uuidExtractor, entityAs(jsonAs(Group.class)),
                                        (ctx, uuid, group) -> {
                                            if (!Objects.equals(group.getUuid(), uuid))
                                                return ctx.completeWithStatus(StatusCodes.BadRequest());
                                            else {
                                                groups.update(group);
                                                return ctx.completeWithStatus(StatusCodes.OK());
                                            }
                                        }
                                )
                        )),
                        put(path(uuidExtractor).route(
                                        handleWith(uuidExtractor,
                                                (ctx, uuid) -> {
                                                    groups.delete(uuid);
                                                    return ctx.completeWithStatus(StatusCodes.OK());
                                                }
                                        )
                        ))

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
