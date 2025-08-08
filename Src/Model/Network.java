package Model;
import java.util.*;

public class Network {
    public static Queue<Transaction> tempmem;

    static {
        tempmem = new Queue<>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Transaction> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(Transaction transaction) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends Transaction> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public boolean equals(Object o) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public boolean offer(Transaction transaction) {
                return false;
            }

            @Override
            public Transaction remove() {
                return null;
            }

            @Override
            public Transaction poll() {
                return null;
            }

            @Override
            public Transaction element() {
                return null;
            }

            @Override
            public Transaction peek() {
                return null;
            }
        };
    }

    public static void broadcastTransaction(Transaction tx) {
        System.out.println("Phát tán giao dịch lên mạng lưới blockchain");
        tempmem.add(tx);
        System.out.println("Giao dịch đang chờ trong bộ nhớ tạm. Số giao dịch: " + tempmem.size());
    }
}

