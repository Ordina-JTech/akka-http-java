package nl.ordina.java.akkahttp;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public class GroupRepo {
  private final ConcurrentHashMap<UUID, Group> groups = new ConcurrentHashMap<>();

  {
    groups.put(UUID.fromString("d0926864-e5e7-4bca-8067-d05eb7c725e9"),
        new Group(UUID.fromString("d0926864-e5e7-4bca-8067-d05eb7c725e9"), "BLa bla"));
  }

  public CompletableFuture<Group> get(UUID uuid) {
    return completedFuture(groups.get(uuid));
  }

  public CompletableFuture<Group> create(Group group) {
    UUID uuid = UUID.randomUUID();
    Group groupWithId = new Group(uuid, group.getName());
    groups.put(uuid, groupWithId);
    return completedFuture(groupWithId);
  }

  public CompletableFuture<Group> update(Group group) {
    groups.put(group.getUuid(), group);
    return completedFuture(group);
  }

  public CompletableFuture<Collection<Group>> getAll(Integer page, Integer pageSize) {
    List<Group> groupPage = groups.values().stream().skip((page - 1) * pageSize).limit(pageSize).collect(toList());
    return completedFuture(groupPage);
  }

  public CompletableFuture<Void> delete(UUID uuid) {
    groups.remove(uuid);
    return completedFuture(null);
  }
}
