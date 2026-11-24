import { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import { useAuctionEvents } from './events.jsx'

// The virtual wallet of the logged-in user ({ balance, reserved, available, transactions }),
// refreshed after every bid or closed auction on the site
export function useWallet(user) {
  const [wallet, setWallet] = useState(null)
  const userId = user?.id

  const reload = useCallback(() => {
    if (!userId) return
    api('/wallet')
      .then(setWallet)
      .catch(() => {})
  }, [userId])

  useEffect(() => {
    reload()
  }, [reload])

  useAuctionEvents((event) => {
    if (event.type === 'bid' || event.type === 'closed') reload()
  })

  return { wallet: userId ? wallet : null, reload }
}
