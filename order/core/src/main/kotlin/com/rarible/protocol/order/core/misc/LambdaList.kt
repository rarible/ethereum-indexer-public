package com.rarible.protocol.order.core.misc

class LambdaList<T>(private val delegate: () -> List<T>) : List<T> {
    override val size: Int
        get() = delegate().size

    override fun get(index: Int): T {
        return delegate()[index]
    }

    override fun isEmpty(): Boolean {
        return delegate().isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return delegate().iterator()
    }

    override fun listIterator(): ListIterator<T> {
        return delegate().listIterator()
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return delegate().listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return delegate().subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: T): Int {
        return delegate().lastIndexOf(element)
    }

    override fun indexOf(element: T): Int {
        return delegate().indexOf(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return delegate().containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return delegate().contains(element)
    }
}
