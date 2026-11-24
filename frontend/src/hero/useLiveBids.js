import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { useAuctionEvents } from '../events.jsx'

// The latest bids on the whole site (newest first), refreshed on every live bid.
// onBid(event) is called for each live bid, so 3D scenes can react to real activity.
export function useLiveBids(onBid) {
  const [bids, setBids] = useState([])
  const onBidRef = useRef(onBid)

  useEffect(() => {
    onBidRef.current = onBid
  })

  const load = useCallback(() => {
    api('/bids/recent', { auth: false })
      .then(setBids)
      .catch(() => {})
  }, [])

  useEffect(() => {
    load()
  }, [load])

  useAuctionEvents((event) => {
    if (event.type !== 'bid') return
    load()
    onBidRef.current?.(event)
  })

  return bids
}
