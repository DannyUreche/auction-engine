import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './api.js'
import { useAuctionEvents } from './events.jsx'

// Live show video over WebRTC, peer to peer: the organizer's browser sends its camera to every viewer.
// Connection messages go through the server's private inboxes (LiveStreamService): a live event "rtc" says
// that a peer has mail, the peer then fetches it. Google's public STUN server finds the network route.
const ICE_SERVERS = [{ urls: 'stun:stun.l.google.com:19302' }]

async function send(showId, from, to, kind, data) {
  await api(`/shows/${showId}/stream/signal`, { method: 'POST', body: { from, to, kind, data: JSON.stringify(data) } })
}

// ICE candidates can arrive before the description they belong to: keep them until it is set
async function addCandidate(pc, pending, candidate) {
  if (pc.remoteDescription) await pc.addIceCandidate(candidate).catch(() => {})
  else pending.push(candidate)
}

async function flushCandidates(pc, pending) {
  while (pending.length) await pc.addIceCandidate(pending.shift()).catch(() => {})
}

// ---------- Organizer ----------
export function useHostVideo(showId) {
  const host = `host-${showId}`
  const [stream, setStream] = useState(null)
  const [viewers, setViewers] = useState(0)
  const [error, setError] = useState('')
  const peers = useRef(new Map()) // viewer id -> { pc, pending }
  const media = useRef(null)
  const draining = useRef(false)

  const countViewers = () => setViewers([...peers.current.values()].filter(({ pc }) => pc.connectionState === 'connected').length)

  const closePeer = (id) => {
    peers.current.get(id)?.pc.close()
    peers.current.delete(id)
    countViewers()
  }

  const drain = useCallback(async () => {
    if (!media.current || draining.current) return
    draining.current = true
    try {
      const signals = await api(`/shows/${showId}/stream/inbox?peer=${host}`)
      for (const signal of signals) {
        if (signal.kind === 'join') {
          closePeer(signal.from)
          const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
          const entry = { pc, pending: [] }
          peers.current.set(signal.from, entry)
          media.current.getTracks().forEach((track) => pc.addTrack(track, media.current))
          pc.onicecandidate = (e) => e.candidate && send(showId, host, signal.from, 'ice', e.candidate).catch(() => {})
          pc.onconnectionstatechange = () => {
            if (['failed', 'closed'].includes(pc.connectionState)) closePeer(signal.from)
            else countViewers()
          }
          await pc.setLocalDescription(await pc.createOffer())
          await send(showId, host, signal.from, 'offer', pc.localDescription)
        } else if (signal.kind === 'leave') {
          closePeer(signal.from)
        } else {
          const entry = peers.current.get(signal.from)
          if (!entry) continue
          const data = JSON.parse(signal.data)
          if (signal.kind === 'answer') {
            await entry.pc.setRemoteDescription(data)
            await flushCandidates(entry.pc, entry.pending)
          } else if (signal.kind === 'ice') {
            await addCandidate(entry.pc, entry.pending, data)
          }
        }
      }
    } catch {
      // a lost message: the viewer asks again on its own after a while
    } finally {
      draining.current = false
    }
  }, [showId, host]) // eslint-disable-line react-hooks/exhaustive-deps

  useAuctionEvents((event) => {
    if (event.type === 'rtc' && event.itemId === showId && event.username === host) drain()
  })

  // Heartbeat + fallback: the server ends the stream if the organizer stops asking for 45 s
  useEffect(() => {
    if (!stream) return
    const id = setInterval(drain, 3000)
    return () => clearInterval(id)
  }, [stream, drain])

  const stopTracks = () => {
    peers.current.forEach(({ pc }) => pc.close())
    peers.current.clear()
    media.current?.getTracks().forEach((track) => track.stop())
    media.current = null
    setStream(null)
    setViewers(0)
  }

  async function start() {
    setError('')
    try {
      const camera = await navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 1280 }, height: { ideal: 720 } }, audio: true })
      media.current = camera
      await api(`/shows/${showId}/stream/start`, { method: 'POST' })
      setStream(camera)
    } catch (err) {
      stopTracks()
      setError(err.name === 'NotAllowedError' ? 'Permite accesul la camera si microfon din browser.' : err.message || 'Camera nu a putut fi pornita.')
    }
  }

  async function stop() {
    stopTracks()
    await api(`/shows/${showId}/stream/stop`, { method: 'POST' }).catch(() => {})
  }

  // Leaving the page ends the broadcast
  useEffect(
    () => () => {
      if (media.current) {
        stopTracks()
        api(`/shows/${showId}/stream/stop`, { method: 'POST' }).catch(() => {})
      }
    },
    [showId], // eslint-disable-line react-hooks/exhaustive-deps
  )

  return { stream, viewers, error, start, stop }
}

// ---------- Viewer ----------
export function useViewerVideo(showId, enabled) {
  const peer = useRef(null)
  if (!peer.current) peer.current = crypto.randomUUID()
  const host = `host-${showId}`
  const [live, setLive] = useState(false)
  const [stream, setStream] = useState(null)
  const [error, setError] = useState('')
  const connection = useRef(null) // { pc, pending }
  const draining = useRef(false)

  // The "stream" live event can be missed (e.g. while the event connection is still opening): check now and then
  useEffect(() => {
    const check = () =>
      api(`/shows/${showId}/stream`, { auth: false })
        .then((status) => setLive(status.live))
        .catch(() => {})
    check()
    const id = setInterval(check, 8000)
    return () => clearInterval(id)
  }, [showId])

  const close = () => {
    connection.current?.pc.close()
    connection.current = null
    setStream(null)
  }

  const drain = useCallback(async () => {
    if (draining.current) return
    draining.current = true
    try {
      const signals = await api(`/shows/${showId}/stream/inbox?peer=${peer.current}`, { auth: false })
      for (const signal of signals) {
        const data = JSON.parse(signal.data)
        if (signal.kind === 'offer') {
          connection.current?.pc.close()
          const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
          const entry = { pc, pending: [] }
          connection.current = entry
          pc.ontrack = (e) => setStream(e.streams[0])
          pc.onicecandidate = (e) => e.candidate && send(showId, peer.current, host, 'ice', e.candidate).catch(() => {})
          pc.onconnectionstatechange = () => {
            if (pc.connectionState === 'failed') setError('Conexiunea video a esuat (reteaua nu permite legatura directa).')
          }
          await pc.setRemoteDescription(data)
          await flushCandidates(pc, entry.pending)
          await pc.setLocalDescription(await pc.createAnswer())
          await send(showId, peer.current, host, 'answer', pc.localDescription)
        } else if (signal.kind === 'ice' && connection.current) {
          await addCandidate(connection.current.pc, connection.current.pending, data)
        }
      }
    } catch {
      // try again on the next nudge
    } finally {
      draining.current = false
    }
  }, [showId, host])

  useAuctionEvents((event) => {
    if (event.itemId !== showId) return
    if (event.type === 'stream') setLive(Number(event.amount) === 1)
    if (event.type === 'rtc' && event.username === peer.current) drain()
  })

  // Ask for the video while it is on; leave when it stops or the page closes
  useEffect(() => {
    if (!live || !enabled) {
      close()
      return
    }
    setError('')
    const join = () =>
      api(`/shows/${showId}/stream/join`, { method: 'POST', auth: false, body: { peer: peer.current } }).catch((err) => setError(err.message))
    join()
    // No offer yet: look in the inbox, and every few tries ask again (the host may have missed the join)
    let tries = 0
    const id = setInterval(() => {
      if (connection.current) return
      tries += 1
      if (tries % 3 === 0) join()
      drain()
    }, 2500)
    return () => {
      clearInterval(id)
      close()
      fetch(`/api/shows/${showId}/stream/leave`, {
        method: 'POST',
        keepalive: true,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ peer: peer.current }),
      }).catch(() => {})
    }
  }, [live, enabled, showId, drain]) // eslint-disable-line react-hooks/exhaustive-deps

  return { live, stream, error }
}
