package nl.ordina.java.akkahttp;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class GroupRepo {
  private final ConcurrentHashMap<UUID, Group> groups = new ConcurrentHashMap<>();

  {
    groups.put(UUID.fromString("d0926864-e5e7-4bca-8067-d05eb7c725e9"),
        new Group(UUID.fromString("d0926864-e5e7-4bca-8067-d05eb7c725e9"), "BLa bla"));
  }

  public Group get(UUID uuid) {
    return groups.get(uuid);
  }

  public Group create(Group group) {
    UUID uuid = UUID.randomUUID();
    Group groupWithId = new Group(uuid, group.getName());
    groups.put(uuid, groupWithId);
    return groupWithId;
  }

  public void update(Group group) {
    groups.put(group.getUuid(), group);
  }

  public CompletableFuture<Collection<Group>> getAll() {
    return completedFuture(groups.values());
  }

  public void delete(UUID uuid) {
    groups.remove(uuid);
  }
}
