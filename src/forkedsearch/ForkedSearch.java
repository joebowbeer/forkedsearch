package forkedsearch;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ForkedSearch {

    private static final ForkJoinPool pool = new ForkJoinPool();

    public static void main(String[] args) {
        String[] data = new String[65536];
        Arrays.fill(data, "");
        data[new Random().nextInt(data.length)] = "HERE";
        System.out.println("Result is " + pool.invoke(Search.newTask(data)));
    }

    private static class Search {

        private final String[] data;

        public static Searcher newTask(String[] data) {
            return new Search(data).new Searcher(0, data.length, null);
        }

        protected Search(String[] data) {
            this.data = data;
        }

        private class Searcher extends RecursiveTask<String> {

            private final int begin;
            private final int end;
            private final Searcher next; // keeps track of forked tasks

            protected Searcher(int begin, int end, Searcher next) {
                this.begin = begin;
                this.end = end;
                this.next = next;
            }

            @Override
            protected String compute() {
                int lo = begin;
                int hi = end;
                Searcher right = null;
                while (!isCancelled()
                        && hi - lo > 1
                        && getSurplusQueuedTaskCount() <= 3) {
                    int mid = (lo + hi) >>> 1;
                    right = new Searcher(mid, hi, right);
                    right.fork();
                    hi = mid;
                }
                String result = search(lo, hi);
                while (!isCancelled()
                        && result == null && right != null) {
                    if (right.tryUnfork()) {
                        // search directly if not stolen
                        result = right.search(right.begin, right.end);
                    } else {
                        result = right.join();
                    }
                    right = right.next;
                }
                // got result or cancelled: cancel remaining tasks
                while (right != null) {
                    right.cancel(false);
                    right = right.next;
                }
                return result;
            }

            /**
             * Searches within specified range.
             *
             * @param lo beginning index, inclusive
             * @param hi ending index, exclusive
             * @return matching string if found, else <code>null</code>
             */
            protected String search(int lo, int hi) {
                for (int i = lo; i < hi && !isCancelled(); i++) {
                    String s = data[i];
                    if (matches(s)) {
                        return s;
                    }
                }
                return null;
            }

            private boolean matches(String s) {
                try { Thread.sleep(1); } catch (InterruptedException ex) { }
                return s.length() > 0;
            }
        }
    }
}
