package com.github.steank.ethylene.collection;

import com.github.steank.ethylene.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>Contains functionality and methods common to many {@link ConfigList} implementations. This abstract class does not
 * define any abstract methods. Its main use is to enable concrete implementations to specify what kind of backing list
 * should be used.</p>
 *
 * <p>Subclasses must take care to ensure that the list used to construct this object does not contain null elements at
 * any point. Therefore, the backing list should not be exposed anywhere where it may be accidentally used. See
 * {@link ArrayConfigList} for an example of how to properly inherit this class.</p>
 */
public abstract class AbstractConfigList extends AbstractList<ConfigElement> implements ConfigList {
    private final List<ConfigElement> list;

    /**
     * Construct a new AbstractConfigList using the provided list to store its elements.
     * @param list the backing list
     * @throws NullPointerException if list is null
     */
    protected AbstractConfigList(@NotNull List<ConfigElement> list) {
        this.list = Objects.requireNonNull(list);
    }

    @Override
    public boolean add(@NotNull ConfigElement element) {
        return list.add(Objects.requireNonNull(element));
    }

    @Override
    public void add(int index, @NotNull ConfigElement element) {
        list.add(index, Objects.requireNonNull(element));
    }

    @Override
    public @NotNull ConfigElement set(int index, @NotNull ConfigElement element) {
        return list.set(index, Objects.requireNonNull(element));
    }

    @Override
    public @NotNull ConfigElement remove(int index) {
        return list.remove(index);
    }

    @Override
    public @NotNull ConfigElement get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    /**
     * This helper method can be used to construct a list with the same elements as a collection. If the collection
     * contains any null values, a {@link NullPointerException} will be thrown.
     * @param collection The collection whose elements will be added to the new list
     * @param listSupplier The supplier used to create the list
     * @param <T> the type of the list to construct
     * @return a list, constructed by the supplier, and containing the same elements as collection
     * @throws NullPointerException if collection contains any null elements
     */
    protected static <T extends List<ConfigElement>> T constructList(@NotNull Collection<ConfigElement> collection,
                                                                     @NotNull Supplier<T> listSupplier) {
        Objects.requireNonNull(collection);
        Objects.requireNonNull(listSupplier);

        T newList = listSupplier.get();
        for(ConfigElement element : collection) {
            newList.add(Objects.requireNonNull(element, "input collection must not contain null elements"));
        }

        return newList;
    }
}