package util;

public class MyLinkedList<Type> {
    class MyLinkedNode<Type> {
        Type value;
        MyLinkedNode<Type> next;

        MyLinkedNode(Type _value) {
            value = _value;
            next = null;
        }
    }

    MyLinkedNode<Type> head;
    MyLinkedNode<Type> tail;
    int size;
    MyLinkedNode<Type> pointer; // for loop only

    public MyLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    public int size() {
        return size;
    }

    public void add(Type _value) {
        MyLinkedNode node = new MyLinkedNode(_value);
        // the first element
        if (tail == null) {
            head = node;
            tail = node;
        }
        else {
            tail.next = node;
            tail = node;
        }
        size ++;
    }

    public void remove(Type _value) {
        MyLinkedNode curr = head;
        // to remove head
        if (curr.value == _value) {
            head = curr.next;
            size --;
            if (pointer == curr) {
                pointer = curr.next;
            }
            return;
        }
        MyLinkedNode pre = curr;
        curr = curr.next;
        while (curr != null) {
            if (curr.value == _value) {
                pre.next = curr.next;
                size --;
                if (pointer == curr) {
                    pointer = curr.next;
                }
            }
            pre = curr;
            curr = curr.next;
        }
    }

    public void startLoop() {
        pointer = head;
    }

    public Type next() {
        if (pointer == null) {
            return null;
        }
        Type value = pointer.value;
        pointer = pointer.next;
        return value;
    }

    public void print(boolean newline) {
        System.out.print("[");
        MyLinkedNode curr = head;
        while (curr != null) {
            if (newline) {
                System.out.println();
            }
            else {
                System.out.print(" ");
            }
            System.out.print(curr.value);
            curr = curr.next;
        }
        System.out.print(" ]");
    }
}
