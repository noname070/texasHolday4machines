package ru.noname070.pockerroom.util;

public class Pair<R, S> {
    private R p1;
    private S p2;

    public R getFirst() {
        return this.p1;
    }

    public S getSecond() {
        return this.p2;
    }

    public Pair(R p1, S p2) {
        this.p1 = p1;
        this.p2 = p2;
    }
}
