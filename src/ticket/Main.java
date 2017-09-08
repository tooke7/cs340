package ticket;

import clojure.java.api.Clojure;
import clojure.lang.*;

import java.util.concurrent.Callable;

public class Main {
    private static final IPersistentMap EXAMPLE_DATA = (IPersistentMap)EdnReader.readString(
            "{\"users\" [{\"name\" \"John\" \"password\" \"abc123\"} " +
                    "{\"name\" \"Suzy\" \"password\" \"correcthorsebatterystaple\"}]}",
            PersistentHashMap.EMPTY);
    static final Ref state = new Ref(EXAMPLE_DATA);

    static final IFn assocIn = Clojure.var("clojure.core", "assoc-in");
    static final IFn getIn = Clojure.var("clojure.core", "get-in");
    static final IFn hashMap = Clojure.var("clojure.core", "hash-map");

    public static void main(String[] args) {
        System.out.println(state.deref());

        User john = User.getUser(0);
        System.out.println(john);
        System.out.println(john.getName());
        System.out.println(john.getPassword());

        User fred = User.createUser("Fred", "ilikecheese");
        User.setUser(fred, 2);
        System.out.println(state.deref());
    }

    public static Object assoc(Object value, Object... path) {
        return assocIn.invoke(state.deref(), path, value);
    }

    public static Object get(Object... path) {
        return getIn.invoke(state.deref(), path);
    }

    public static void updateState(Callable c) {
        try {
            LockingTransaction.runInTransaction(c);
        } catch (Exception e) {
            System.out.println("Couldn't update state");
            e.printStackTrace();
        }
    }
}