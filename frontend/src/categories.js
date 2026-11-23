import { useEffect, useState } from 'react'
import { api } from './api.js'

// Lista categoriilor vine de la server (GET /api/categories) si se incarca o singura data
let cache = null

function loadCategories() {
  if (!cache) {
    cache = api('/categories', { auth: false }).catch((err) => {
      cache = null // la eroare reincercam data viitoare
      throw err
    })
  }
  return cache
}

// [{ code, label, icon }]
export function useCategories() {
  const [categories, setCategories] = useState([])
  useEffect(() => {
    let active = true
    loadCategories()
      .then((list) => active && setCategories(list))
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])
  return categories
}

// "ELECTRONICE" -> { code, label, icon } (sau un rezultat de rezerva cat timp lista nu s-a incarcat)
export function findCategory(categories, code) {
  return categories.find((c) => c.code === code) ?? { code, label: code === 'ALTELE' ? 'Altele' : code, icon: '📦' }
}
