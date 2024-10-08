package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * <p>Contains functionality and methods common to many {@link ConfigList} implementations. This abstract class does
 * not define any abstract methods. Its main use is to enable concrete implementations to specify what kind of backing
 * list should be used.</p>
 *
 * <p>Subclasses must take care to ensure that the list used to construct this object does not contain null elements at
 * any point. Therefore, the backing list should not be exposed anywhere where it may be accidentally used. See
 * {@link ArrayConfigList} for an example of how to properly inherit this class.</p>
 */
public abstract class BackedConfigList extends AbstractConfigList {
    /**
     * The backing list for this AbstractConfigList. Implementations that access this variable must take care to ensure
     * that null values cannot be inserted, and the list is never exposed publicly.
     */
    protected final List<ConfigElement> list;

    /**
     * Construct a new AbstractConfigList using the provided list to store its elements.
     *
     * @param list the backing list
     * @throws NullPointerException if list is null
     */
    protected BackedConfigList(@NotNull List<ConfigElement> list) {
        this.list = Objects.requireNonNull(list);
    }

    /**
     * This helper method can be used to construct a list with the same elements as a collection. If the collection
     * contains any null values, a {@link NullPointerException} will be thrown.
     *
     * @param collection   the collection whose elements will be added to the new list
     * @param listSupplier the supplier used to create the new list from the size of the original collection
     * @return a list, constructed by the supplier, and containing the same elements as collection
     * @throws NullPointerException if any of the arguments are null, collection contains any null elements, or
     *                              listSupplier returns null
     */
    protected static @NotNull List<ConfigElement> constructList(@NotNull Collection<? extends ConfigElement> collection,
        @NotNull IntFunction<? extends List<ConfigElement>> listSupplier) {
        Objects.requireNonNull(collection);
        Objects.requireNonNull(listSupplier);

        List<ConfigElement> newList = Objects.requireNonNull(listSupplier.apply(collection.size()));
        for (ConfigElement element : collection) {
            newList.add(Objects.requireNonNull(element, "collection element"));
        }

        return newList;
    }

    @Override
    public boolean add(@NotNull ConfigElement element) {
        return list.add(Objects.requireNonNull(element));
    }

    @Override
    public @NotNull ConfigElement get(int index) {
        return list.get(index);
    }

    @Override
    public @NotNull ConfigElement set(int index, @NotNull ConfigElement element) {
        return list.set(index, Objects.requireNonNull(element));
    }

    @Override
    public void add(int index, @NotNull ConfigElement element) {
        list.add(index, Objects.requireNonNull(element));
    }

    @Override
    public @NotNull ConfigElement remove(int index) {
        return list.remove(index);
    }

    @Override
    public int size() {
        return list.size();
    }
}