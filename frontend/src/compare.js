import { useEffect, useState } from 'react'

// Items picked for comparison (at most 3), kept in localStorage and shared by every component on the page.
// Stored as { id, name, imageUrl } so the floating tray can show them without loading anything.
export const MAX_COMPARE = 3
const KEY = 'compare'
const listeners = new Set()

function read() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) ?? []
  } catch {
    return []
  }
}

function write(items) {
  localStorage.setItem(KEY, JSON.stringify(items))
  listeners.forEach((listener) => listener(items))
}

export function useCompare() {
  const [items, setItems] = useState(read)
  useEffect(() => {
    listeners.add(setItems)
    // Another tab changed the list
    const onStorage = (event) => event.key === KEY && setItems(read())
    window.addEventListener('storage', onStorage)
    return () => {
      listeners.delete(setItems)
      window.removeEventListener('storage', onStorage)
    }
  }, [])

  return {
    items,
    has: (id) => items.some((item) => item.id === id),
    full: items.length >= MAX_COMPARE,
    toggle: (item) => {
      const current = read()
      if (current.some((existing) => existing.id === item.id)) write(current.filter((existing) => existing.id !== item.id))
      else if (current.length < MAX_COMPARE) write([...current, { id: item.id, name: item.name, imageUrl: item.imageUrl }])
    },
    remove: (id) => write(read().filter((item) => item.id !== id)),
    clear: () => write([]),
  }
}
