package nl.ordina.java.akkahttp;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.*;
import akka.http.javadsl.server.values.Parameter;
import akka.http.javadsl.server.values.Parameters;
import akka.http.javadsl.server.values.PathMatcher;
import com.google.inject.Guice;
import com.google.inject.Injector;
import scala.collection.Seq;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BiFunction;

import static akka.http.javadsl.marshallers.jackson.Jackson.json;
import static akka.http.javadsl.marshallers.jackson.Jackson.jsonAs;
import static akka.http.javadsl.model.HttpResponse.create;
import static akka.http.javadsl.server.RequestVals.entityAs;
import static akka.http.javadsl.server.values.Parameters.intValue;
import static akka.http.javadsl.server.values.PathMatchers.uuid;
import static akka.http.scaladsl.model.StatusCodes.*;
import static scala.compat.java8.FutureConverters.toScala;

public class Server extends HttpApp {


  private final GroupRepo repo;

  @Inject
  public Server(GroupRepo repo) {
    this.repo = repo;
  }

  /* Dezelfde instantie moet gebruikt worden voor het opvragen van een PathMatcher! */
  PathMatcher<UUID> uuid = uuid();

  BiFunction<RequestContext, Object, RouteResult> b = (RequestContext ctx, Object asMarshalled) -> ctx.completeAs(json(), asMarshalled);


  @Override
  public Route createRoute() {


    return handleExceptions(e -> {
          e.printStackTrace();
          return complete(create().withStatus(InternalServerError()));
        },
        pathSingleSlash().route(
            getFromResource("web/index.html")
        ),
        pathPrefix("groups").route(
            getAll(),
            get(path(uuid).route(get())),
            put(path(uuid).route(updateGroup())),
            put(path(uuid).route(deleteGroup())),
            post(saveGroup())
        )
    );
  }

  private Route getAll() {
    return get(pathEndOrSingleSlash().route(
        getFirst()
    ));
  }

  private Route getFirst() {
    RequestVal<Integer> pageE = intValue("page").withDefault(1);
    RequestVal<Integer> pageSizeE = intValue("pageSize").withDefault(10);

    return extractHere(pageE, pageSizeE).route(
        handleWith(pageE, pageSizeE,
            (ctx, page, pageSize) -> ctx.completeWith(
                toScala(
                    repo.getAll(page, pageSize)
                        .thenApplyAsync(groups -> ctx.completeAs(json(), groups)))
            ))
    );
  }

  private Route get() {
    return handleWith(uuid,
        (ctx, uuid) ->
            ctx.completeWith(
                toScala(
                    repo.get(uuid)
                        .thenApplyAsync(g -> ctx.completeAs(json(), g))))

    );
  }

  private Route deleteGroup() {
    return handleWith(uuid,
        (ctx, uuid) ->
            ctx.completeWith(
                toScala(
                    repo.delete(uuid)
                        .supplyAsync(() -> completeWithOk(ctx))))
    );
  }

  private Route updateGroup() {
    return handleWith(uuid, entityAs(jsonAs(Group.class)),
        (ctx, uuid, group) ->
            ctx.completeWith(
                toScala(
                    repo.update(new Group(uuid, group.getName()))
                        .thenApplyAsync(g -> completeWithOk(ctx))
                )
            )
    );
  }


  private Route saveGroup() {
    return handleWith(entityAs(jsonAs(Group.class)),
        (RequestContext ctx, Group group) ->
            ctx.completeWith(
                toScala(repo.create(group)
                        .thenApplyAsync(g -> created(createHeader(ctx, g)))
                        .thenApplyAsync(ctx::complete)
                )
            )
    );
  }

  private RouteResult completeWithOk(RequestContext ctx) {
    return ctx.complete(HttpResponse.create().withStatus(OK()));
  }

  private HttpResponse created(Location header) {
    return
        HttpResponse
            .create()
            .withStatus(Created())
            .addHeader(header);
  }

  private Location createHeader(RequestContext ctx, Group g) {
    Uri uri = ctx.request().getUri().addPathSegment(g.getUuid().toString());
    return Location.create(uri);
  }

  public static void main(String[] args) throws IOException {
    ActorSystem akkaSystem = ActorSystem.create("akka-http-example");
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(Server.class).bindRoute("localhost", 8080, akkaSystem);

    System.out.println("<ENTER> to exit!");
    System.in.read();
    akkaSystem.shutdown();
  }
}
