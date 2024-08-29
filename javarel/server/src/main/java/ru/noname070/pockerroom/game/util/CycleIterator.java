package ru.noname070.pockerroom.game.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CycleIterator<R> implements Iterator<R> {

    private List<R> names;
    private int conunt = 0;
    
    @SuppressWarnings("unchecked") // да похуй
    public CycleIterator(R...names) {
        this.names = new ArrayList<R>();
        this.names.addAll(Arrays.asList(names));
    }

    @Override
    public boolean hasNext() {
        return names.size() > 0;
    }

    public void remove(R object) {
        this.names.remove(object);
    }

    @Override
    public R next() {
        return this.names.get((conunt += 1) % this.names.size());
    }

}
