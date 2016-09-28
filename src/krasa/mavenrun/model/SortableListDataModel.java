package krasa.mavenrun.model;

import krasa.mavenrun.analyzer.MyListNode;

import javax.swing.DefaultListModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by alin.simionoiu on 9/23/16.
 */
public class SortableListDataModel {
    private final List<MyListNode> delegate;


    public SortableListDataModel() {
        delegate = new ArrayList<>();
    }

    public void add(MyListNode node) {
        delegate.add(node);
    }

    public boolean isEmpty() {
        return (delegate == null || delegate.size() == 0);
    }

    public void populateListSorted(DefaultListModel<MyListNode> list) {
        Collections.sort(delegate, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String first = ((MyListNode) o1).toString();
                String second = ((MyListNode) o2).toString();

                return first.split(":")[1].compareTo(second.split(":")[1]);
            }
        });

        for (MyListNode node : delegate) {
            list.addElement(node);
        }
    }
}
