package nl.ordina.java.akkahttp;

import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.*;
import akka.http.javadsl.server.values.PathMatcher;

import javax.inject.Inject;

import java.util.UUID;

import static akka.http.javadsl.marshallers.jackson.Jackson.json;
import static akka.http.javadsl.marshallers.jackson.Jackson.jsonAs;
import static akka.http.javadsl.server.RequestVals.entityAs;
import static akka.http.javadsl.server.values.Parameters.intValue;
import static akka.http.javadsl.server.values.PathMatchers.uuid;
import static akka.http.scaladsl.model.StatusCodes.Created;
import static akka.http.scaladsl.model.StatusCodes.OK;
import static scala.compat.java8.FutureConverters.toScala;

public class GroupHttp extends AllDirectives {
  private final GroupRepo repo;

  @Inject
  public GroupHttp(GroupRepo repo) {
    this.repo = repo;
  }

  /* Dezelfde instantie moet gebruikt worden voor het opvragen van een PathMatcher! */
  PathMatcher<UUID> uuid = uuid();


  Route groups() {
    return pathPrefix("groups").route(
        getAll(),
        get(path(uuid).route(get())),
        put(path(uuid).route(updateGroup())),
        put(path(uuid).route(deleteGroup())),
        post(saveGroup())
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
                        .thenApplyAsync(g ->
                            g == null ? ctx.notFound() : ctx.completeAs(json(), g))))

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

}
