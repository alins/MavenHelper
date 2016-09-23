package krasa.mavenrun.model;

import javax.swing.*;
import java.util.*;

/**
 * Created by alin.simionoiu on 9/23/16.
 */
public class SortableListDataModel<E> extends AbstractListModel<E> {

    private List<E> delegate = new ArrayList<E>();

    /**
     * Returns the number of components in this list.
     * <p>
     * This method is identical to <code>size</code>, which implements the
     * <code>List</code> interface defined in the 1.2 Collections framework.
     * This method exists in conjunction with <code>setSize</code> so that
     * <code>size</code> is identifiable as a JavaBean property.
     *
     * @return  the number of components in this list
     * @see #size()
     */
    public int getSize() {
        return delegate.size();
    }

    /**
     * Returns the component at the specified index.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>get(int)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     * @param      index   an index into this list
     * @return     the component at the specified index
     * @exception  ArrayIndexOutOfBoundsException  if the <code>index</code>
     *             is negative or greater than the current size of this
     *             list
     */
    public E getElementAt(int index) {
        return delegate.get(index);
    }

    /**
     * Copies the components of this list into the specified array.
     * The array must be big enough to hold all the objects in this list,
     * else an <code>IndexOutOfBoundsException</code> is thrown.
     *
     * @param   anArray   the array into which the components get copied
     * @see Vector#copyInto(Object[])
     */
    public void copyInto(Object anArray[]) {
        delegate.toArray(anArray);
    }

    /**
     * Returns the number of components in this list.
     *
     * @return  the number of components in this list
     * @see Vector#size()
     */
    public int size() {
        return delegate.size();
    }

    /**
     * Tests whether this list has any components.
     *
     * @return  <code>true</code> if and only if this list has
     *          no components, that is, its size is zero;
     *          <code>false</code> otherwise
     * @see Vector#isEmpty()
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * Tests whether the specified object is a component in this list.
     *
     * @param   elem   an object
     * @return  <code>true</code> if the specified object
     *          is the same as a component in this list
     * @see Vector#contains(Object)
     */
    public boolean contains(Object elem) {
        return delegate.contains(elem);
    }

    /**
     * Searches for the first occurrence of <code>elem</code>.
     *
     * @param   elem   an object
     * @return  the index of the first occurrence of the argument in this
     *          list; returns <code>-1</code> if the object is not found
     * @see Vector#indexOf(Object)
     */
    public int indexOf(Object elem) {
        return delegate.indexOf(elem);
    }

    /**
     * Returns the index of the last occurrence of <code>elem</code>.
     *
     * @param   elem   the desired component
     * @return  the index of the last occurrence of <code>elem</code>
     *          in the list; returns <code>-1</code> if the object is not found
     * @see Vector#lastIndexOf(Object)
     */
    public int lastIndexOf(Object elem) {
        return delegate.lastIndexOf(elem);
    }

    /**
     * Returns the component at the specified index.
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index
     * is negative or not less than the size of the list.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is <code>get(int)</code>, which implements the
     *    <code>List</code> interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      index   an index into this list
     * @return     the component at the specified index
     * @see Vector#elementAt(int)
     */
    public E elementAt(int index) {
        return delegate.get(index);
    }

    /**
     * Returns the first component of this list.
     * Throws a <code>NoSuchElementException</code> if this
     * vector has no components.
     * @return     the first component of this list
     * @see Vector#firstElement()
     */
    public E firstElement() {
        return delegate.get(0);
    }

    /**
     * Returns the last component of the list.
     * Throws a <code>NoSuchElementException</code> if this vector
     * has no components.
     *
     * @return  the last component of the list
     * @see Vector#lastElement()
     */
    public E lastElement() {
        if (delegate == null) return null;
        return delegate.get(delegate.size());
    }

    public void addElement(E element) {
        delegate.add(element);
    }

    public void clear() {
        delegate = new ArrayList<E>();
    }

    public void sort(Comparator<E> comparator) {
        Collections.sort(delegate, comparator);
    }
}
