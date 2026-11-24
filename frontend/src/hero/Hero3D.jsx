import { useRef } from 'react'
import LiveTicker from './LiveTicker.jsx'
import ShowcaseScene from './ShowcaseScene.jsx'
import { useLiveBids } from './useLiveBids.js'

// The 3D part of the home page hero: the showcase carousel of active auctions + the latest-bid ticker
export default function Hero3D({ items }) {
  // The scene registers here what it does on a live bid (puts that item in the spotlight)
  const onLiveBid = useRef(null)
  const bids = useLiveBids((event) => onLiveBid.current?.(event))

  return (
    <div className="relative h-[340px] sm:h-[420px] lg:h-[440px]">
      <ShowcaseScene items={items} onLiveBid={onLiveBid} />
      <div className="pointer-events-none absolute inset-x-0 bottom-1 flex justify-center px-2">
        <LiveTicker bids={bids} />
      </div>
    </div>
  )
}
