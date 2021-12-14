package util;

public class MyLinkedListTest {
    public static void main(String[] args) {

        // test 1 - loop empty list
        MyLinkedList<Integer> l1 = new MyLinkedList<>();
        l1.startLoop();
        Integer x;
        System.out.print("test1: [");
        while ((x = l1.next()) != null) {
            System.out.print(" " + x);
        }
        System.out.print("]");
        System.out.println();

        // test 2 - [1, 2, 3, 4, 5]
        // loop to 1 remove 1
        // expected [ 2 3 4 5 ]
        MyLinkedList<Integer> l2 = new MyLinkedList<>();
        for (int i = 1; i <= 5; i ++) {
            l2.add(i);
        }
        l2.startLoop();
        while ((x = l2.next()) != null) {
            if (x == 1) {
                l2.remove(1);
            }
        }
        System.out.print("test2: ");
        l2.print(false);
        System.out.println();

        // test 3 - [1, 2, 3, 4, 5]
        // loop to 1 remove 2
        // expected [ 1 3 4 5 ]
        MyLinkedList<Integer> l3 = new MyLinkedList<>();
        for (int i = 1; i <= 5; i ++) {
            l3.add(i);
        }
        l3.startLoop();
        while ((x = l3.next()) != null) {
            if (x == 1) {
                l3.remove(2);
            }
        }
        System.out.print("test3: ");
        l3.print(false);
        System.out.println();

        // test 4 - [1, 2, 3, 4, 5]
        // loop to 3 remove 2,3,4
        // expected [ 1 5 ]
        MyLinkedList<Integer> l4 = new MyLinkedList<>();
        for (int i = 1; i <= 5; i ++) {
            l4.add(i);
        }
        l4.startLoop();
        while ((x = l4.next()) != null) {
            if (x == 3) {
                l4.remove(2);
                l4.remove(3);
                l4.remove(4);
            }
        }
        System.out.print("test4: ");
        l4.print(false);
        System.out.println();
    }
}
